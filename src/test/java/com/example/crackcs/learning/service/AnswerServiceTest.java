package com.example.crackcs.learning.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.*;
import com.example.crackcs.content.question.repository.*;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.member.domain.*;
import com.example.crackcs.member.repository.MemberRepository;
import com.example.crackcs.learning.repository.AnswerRepository;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.evaluation.domain.*;
import com.example.crackcs.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "crackcs.evaluation.worker-enabled=false")
@ActiveProfiles("test")
class AnswerServiceTest {
    @Autowired AnswerService service;
    @Autowired EvaluationProcessor processor;
    @Autowired MemberRepository members;
    @Autowired TopicRepository topics;
    @Autowired ConceptRepository concepts;
    @Autowired QuestionRepository questions;
    @Autowired QuestionConceptRepository questionConcepts;
    @Autowired AnswerRepository answers;
    @Autowired EvaluationRepository evaluations;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired ControlledPort port;

    @AfterEach
    void tearDown() {
        port.reset();
        jdbc.update("delete from evaluation_concept");
        evaluations.deleteAllInBatch();
        answers.deleteAllInBatch();
        questionConcepts.deleteAllInBatch();
        questions.deleteAllInBatch();
        concepts.deleteAllInBatch();
        topics.deleteAllInBatch();
        members.deleteAllInBatch();
    }

    @Test
    @DisplayName("답변 저장을 커밋한 뒤 평가 작업을 처리하고 결과를 저장한다")
    void processesCommittedAnswer() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var response = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "  원문 답변  ");
        var pending = evaluations.findByAnswerId(response.answerId()).orElseThrow();
        assertThat(answers.findById(response.answerId()).orElseThrow().getContent()).isEqualTo("  원문 답변  ");
        assertThat(pending.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);

        processor.process(pending.getId());

        var saved = evaluations.findById(pending.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(saved.getScore()).isEqualTo(100);
        assertThat(service.findEvaluation(member.getId(), response.answerId()).concepts()).hasSize(1);
        assertThat(port.allCallsOutsideTransaction).isTrue();
    }

    @Test
    @DisplayName("같은 요청 키의 다른 원문은 충돌이고 새 키의 재답변은 새 이력이다")
    void distinguishesRetryFromNewAnswer() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        String key = UUID.randomUUID().toString();
        var first = service.submit(member.getId(), question.getId(), key, "첫 답변");
        assertThat(service.submit(member.getId(), question.getId(), key, "첫 답변").answerId()).isEqualTo(first.answerId());
        assertThatThrownBy(() -> service.submit(member.getId(), question.getId(), key, "다른 답변"))
                .isInstanceOf(AnswerConflictException.class);
        var second = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "재답변");
        assertThat(service.findAll(member.getId(), PageRequest.of(0, 1)).getContent())
                .extracting(answer -> answer.answerId()).containsExactly(second.answerId());
        assertThat(answers.findById(first.answerId()).orElseThrow().getContent()).isEqualTo("첫 답변");
        assertThat(answers.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("답변 목록은 질문과 평가 및 Concept을 페이지 단위로 일괄 조회한다")
    void loadsAnswerPageWithoutPerAnswerQueries() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var first = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "첫 답변");
        var second = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "둘째 답변");
        processor.process(first.evaluationId());
        processor.process(second.evaluationId());
        var statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        var page = service.findAll(member.getId(), PageRequest.of(0, 1));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().evaluation().concepts()).hasSize(1);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("문제가 폐기되어도 이전 답변의 조회와 동일 요청 복구는 유지한다")
    void retainsHistoryAfterRetirement() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        String key = UUID.randomUUID().toString();
        var first = service.submit(member.getId(), question.getId(), key, "이전 답변");
        question.retire();
        questions.save(question);
        assertThat(service.findById(member.getId(), first.answerId()).questionContent()).isEqualTo(question.getContent());
        assertThat(service.submit(member.getId(), question.getId(), key, "이전 답변").answerId()).isEqualTo(first.answerId());
        assertThatThrownBy(() -> service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "새 답변"))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    @DisplayName("중복 평가 작업은 이미 확정된 평가를 변경하지 않는다")
    void finalizesOnlyOnce() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var answer = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "답변");
        Long evaluationId = evaluations.findByAnswerId(answer.answerId()).orElseThrow().getId();
        processor.process(evaluationId);
        var finalized = evaluations.findById(evaluationId).orElseThrow();
        processor.process(evaluationId);
        var reloaded = evaluations.findById(evaluationId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(finalized.getUpdatedAt());
        assertThat(jdbc.queryForObject("select count(*) from evaluation_concept", Long.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("평가가 세 번 실패하면 답변은 보존하고 실패 원인만 안전하게 저장한다")
    void preservesAnswerOnFailure() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var answer = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "실패해도 보존할 원문");
        Long evaluationId = evaluations.findByAnswerId(answer.answerId()).orElseThrow().getId();
        port.failuresRemaining = 10;

        processor.process(evaluationId);

        var saved = evaluations.findById(evaluationId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo("PROVIDER_ERROR");
        assertThat(saved.getScore()).isNull();
        assertThat(saved.isKnowledgeStateEligible()).isFalse();
        assertThat(port.calls).isEqualTo(3);
        assertThat(answers.findById(answer.answerId()).orElseThrow().getContent()).isEqualTo("실패해도 보존할 원문");
    }

    @Test
    @DisplayName("일시적인 외부 평가 오류는 같은 평가에서 재시도해 성공한다")
    void retriesTransientFailure() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var answer = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "답변");
        Long evaluationId = evaluations.findByAnswerId(answer.answerId()).orElseThrow().getId();
        port.failuresRemaining = 2;

        processor.process(evaluationId);

        assertThat(evaluations.findById(evaluationId).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(port.calls).isEqualTo(3);
        assertThat(evaluations.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("두 작업자가 같은 평가를 처리해도 외부 평가와 결과 저장은 한 번이다")
    void serializesDuplicateWorkers() throws Exception {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var answer = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "답변");
        Long evaluationId = evaluations.findByAnswerId(answer.answerId()).orElseThrow().getId();
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(2);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Void> process = () -> {
                barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                processor.process(evaluationId);
                return null;
            };
            var first = executor.submit(process);
            var second = executor.submit(process);
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(evaluations.findById(evaluationId).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(port.calls).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from evaluation_concept", Long.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("검토 필요 평가는 점수가 없고 지식 상태 반영에서 제외된다")
    void retainsNeedsReviewWithoutScore() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var answer = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "답변");
        Long id = evaluations.findByAnswerId(answer.answerId()).orElseThrow().getId();
        port.outcome = "NEEDS_REVIEW";

        processor.process(id);

        var result = service.findEvaluation(member.getId(), answer.answerId());
        assertThat(result.status()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
        assertThat(result.score()).isNull();
        assertThat(result.concepts()).allSatisfy(c -> assertThat(c.score()).isNull());
        assertThat(port.calls).isEqualTo(1);
    }

    @Test
    @DisplayName("평가 시간 초과는 일반 외부 오류와 구분해 저장한다")
    void recordsTimeoutSeparately() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var answer = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "답변");
        Long id = evaluations.findByAnswerId(answer.answerId()).orElseThrow().getId();
        port.outcome = "TIMEOUT";

        processor.process(id);

        assertThat(evaluations.findById(id).orElseThrow().getFailureReason()).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(port.calls).isEqualTo(3);
    }

    @Test
    @DisplayName("평가에 사용된 개념은 문제 연결을 제거해도 DB에서 삭제할 수 없다")
    void protectsEvaluatedConceptReference() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var answer = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "답변");
        processor.process(evaluations.findByAnswerId(answer.answerId()).orElseThrow().getId());
        questionConcepts.deleteAllInBatch();

        assertThatThrownBy(() -> concepts.deleteAllInBatch())
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class PortConfiguration {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        ControlledPort controlledPort() { return new ControlledPort(); }
    }

    // Only the external provider's availability is controlled; all domain rules run unchanged.
    static class ControlledPort implements com.example.crackcs.evaluation.port.EvaluationPort {
        int failuresRemaining;
        int calls;
        String outcome = "CORRECT";
        boolean allCallsOutsideTransaction = true;
        @Override
        public com.example.crackcs.evaluation.port.EvaluationResult evaluate(com.example.crackcs.evaluation.port.EvaluationRequest request) {
            calls++;
            allCallsOutsideTransaction &= !org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive();
            if (failuresRemaining-- > 0) throw new IllegalStateException("provider secret must never be exposed");
            return new com.example.crackcs.evaluation.adapter.StubEvaluationAdapter(outcome).evaluate(request);
        }
        void reset() { failuresRemaining = 0; calls = 0; outcome = "CORRECT"; allCallsOutsideTransaction = true; }
    }

    private Question publishedQuestion() {
        Member admin = members.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topics.save(Topic.builder().code("OS").name("운영체제").build());
        Concept concept = concepts.save(Concept.builder().topic(topic).code("THREAD").name("스레드").build());
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content("스레드를 설명하세요").referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.addConcept(concept, BigDecimal.ONE, true);
        question.review(admin);
        question.publish();
        return questions.save(question);
    }
}
