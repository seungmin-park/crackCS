package com.example.crackcs.learning.mastery.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.adapter.StubEvaluationAdapter;
import com.example.crackcs.evaluation.domain.ConceptResult;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationCompletionTransaction;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "crackcs.evaluation.retry-base-delay=0ms",
        "spring.datasource.hikari.connection-init-sql=SET LOCK_TIMEOUT 25"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KnowledgeCompletionFailureTest {
    @Autowired
    private MemberRepository members;

    @Autowired
    private TopicRepository topics;

    @Autowired
    private ConceptRepository concepts;

    @Autowired
    private QuestionRepository questions;

    @Autowired
    private AnswerRepository answers;

    @Autowired
    private EvaluationRepository evaluations;

    @Autowired
    private KnowledgeDocumentRepository documents;

    @Autowired
    private KnowledgeChunkRepository chunks;

    @Autowired
    private KnowledgeStateRepository states;

    @Autowired
    private AppliedEvaluationConceptRepository appliedConcepts;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EvaluationProcessor processor;

    @Autowired
    private EvaluationCompletionTransaction completionTransaction;

    @Autowired
    private KnowledgeStateService knowledge;

    @Autowired
    private ControlledPort port;

    @AfterEach
    void cleanUp() {
        try {
            jdbc.execute("alter table knowledge_state drop constraint if exists test_reject_knowledge_observation");
            appliedConcepts.deleteAllInBatch();
            states.deleteAllInBatch();
            evaluations.deleteAll();
            answers.deleteAllInBatch();
            questions.deleteAll();
            chunks.deleteAllInBatch();
            documents.deleteAllInBatch();
            concepts.deleteAllInBatch();
            topics.deleteAllInBatch();
            members.deleteAllInBatch();
        } finally {
            port.reset();
        }
    }

    @Test
    @DisplayName("영구 저장 오류는 평가를 실패로 확정하고 AI를 다시 호출하지 않는다")
    void failsPermanentStorageErrorsWithoutAnotherProviderCall() {
        Fixture fixture = fixture();
        Long evaluationId = pending(fixture).getId();
        jdbc.execute(
                "alter table knowledge_state add constraint test_reject_knowledge_observation check (attempt_count = 0)");

        processor.process(evaluationId);
        processor.process(evaluationId);

        Evaluation saved = evaluations.findById(evaluationId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo("PERSISTENCE_ERROR");
        assertThat(saved.getAttemptCount()).isEqualTo(1);
        assertThat(saved.getLeaseOwner()).isNull();
        assertThat(port.calls.get()).isEqualTo(1);
        assertThat(states.count()).isZero();
        assertThat(appliedConcepts.count()).isZero();
        Evaluation details = evaluations.findByAnswerId(saved.getAnswer().getId()).orElseThrow();
        assertThat(details.getConcepts()).isEmpty();
        assertThat(details.getEvidence()).isEmpty();
    }

    @Test
    @DisplayName("완료 잠금 재시도를 소진하면 저장 충돌 사유로 예약하고 잠금 해제 후 완료한다")
    void labelsExhaustedLockConflictsAsPersistenceConflicts() throws Exception {
        Fixture fixture = fixture();
        Long initialId = completed(fixture, Verdict.CORRECT);
        completionTransaction.execute(() -> knowledge.applyInCurrentTransaction(initialId));
        Long evaluationId = pending(fixture).getId();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> holder = executor.submit(() -> transactions.executeWithoutResult(status -> {
                jdbc.queryForObject("select id from knowledge_state where member_id = ? and concept_id = ? for update",
                        Long.class, fixture.member().getId(), fixture.concept().getId());
                locked.countDown();
                await(release);
            }));
            try {
                assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
                processor.process(evaluationId);

                Evaluation scheduled = evaluations.findById(evaluationId).orElseThrow();
                assertThat(scheduled.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
                assertThat(scheduled.getFailureReason()).isEqualTo("PERSISTENCE_CONFLICT");
                assertThat(scheduled.getAttemptCount()).isEqualTo(1);
                assertThat(port.calls.get()).isEqualTo(1);
                assertThat(states.findByMemberIdAndConceptId(fixture.member().getId(), fixture.concept().getId())
                        .orElseThrow().getAttemptCount()).isEqualTo(1);
                assertThat(appliedConcepts.count()).isEqualTo(1);
            } finally {
                release.countDown();
            }
            holder.get(5, TimeUnit.SECONDS);
        }

        processor.process(evaluationId);

        assertThat(evaluations.findById(evaluationId).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(port.calls.get()).isEqualTo(2);
        assertThat(states.findByMemberIdAndConceptId(fixture.member().getId(), fixture.concept().getId())
                .orElseThrow().getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("AI 제공자 오류는 기존 재시도 정책과 안전한 오류 사유를 유지한다")
    void preservesProviderFailureRetries() {
        Fixture fixture = fixture();
        Long evaluationId = pending(fixture).getId();
        port.unavailable = true;

        processor.process(evaluationId);

        Evaluation retrying = evaluations.findById(evaluationId).orElseThrow();
        assertThat(retrying.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(retrying.getFailureReason()).isEqualTo("PROVIDER_ERROR");
        processor.process(evaluationId);
        processor.process(evaluationId);

        Evaluation failed = evaluations.findById(evaluationId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("PROVIDER_ERROR");
        assertThat(port.calls.get()).isEqualTo(3);
        assertThat(states.count()).isZero();
        assertThat(appliedConcepts.count()).isZero();
    }

    private static void await(CountDownLatch signal) {
        try {
            if (!signal.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("lock release timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    @TestConfiguration
    static class PortConfiguration {
        @Bean
        @Primary
        ControlledPort controlledPort() {
            return new ControlledPort();
        }
    }

    static class ControlledPort implements EvaluationPort {
        private final AtomicInteger calls = new AtomicInteger();
        private boolean unavailable;

        @Override
        public EvaluationResult evaluate(EvaluationRequest request) {
            calls.incrementAndGet();
            if (unavailable) {
                throw new IllegalStateException("provider detail must not escape");
            }
            return new StubEvaluationAdapter("CORRECT").evaluate(request);
        }

        void reset() {
            calls.set(0);
            unavailable = false;
        }
    }

    private Fixture fixture() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Member admin = members.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topics.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeDocument document = KnowledgeDocument.builder().topic(topic).createdByMember(admin)
                .title("스레드 근거").sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성")
                .content("스레드는 프로세스 자원을 공유하는 실행 단위다.").build();
        document.review(admin);
        document.publish();
        document = documents.save(document);
        KnowledgeChunk chunk = chunks.save(KnowledgeChunk.create(document, 0, 0, document.getContent().length(),
                document.getContent(), "test-v1"));
        return new Fixture(member, admin, topic, concept, question, chunk);
    }

    private Concept concept(Topic topic, String name) {
        return concepts.save(Concept.builder().topic(topic).code(UUID.randomUUID().toString()).name(name).build());
    }

    private Question question(Member admin, Topic topic, Concept concept, String content) {
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content(content).referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.addConcept(concept, BigDecimal.ONE, true);
        question.review(admin);
        question.publish();
        return questions.save(question);
    }

    private Evaluation pending(Fixture fixture) {
        return pending(fixture.member(), fixture.question());
    }

    private Evaluation pending(Member member, Question question) {
        Answer answer = answers.save(Answer.builder().member(member).question(question)
                .requestId(UUID.randomUUID().toString()).content("스레드는 실행 단위").build());
        return evaluations.save(Evaluation.builder().answer(answer).build());
    }

    private Long completed(Fixture fixture, Verdict verdict) {
        Long id = pending(fixture).getId();
        complete(id, fixture, verdict);
        return id;
    }

    private void complete(Long id, Fixture fixture, Verdict verdict) {
        transactions.executeWithoutResult(status -> {
            Evaluation evaluation = evaluations.findById(id).orElseThrow();
            evaluation.completeWithEvidence(result(fixture, verdict),
                    List.of(chunks.findById(fixture.chunk().getId()).orElseThrow()));
        });
    }

    private EvaluationResult result(Fixture fixture, Verdict verdict) {
        return new EvaluationResult(verdict, "평가 완료",
                List.of(new ConceptResult(fixture.concept().getId(), verdict, "개념 평가")),
                List.of(), List.of(), List.of(), List.of(fixture.chunk().getId()), "test", "v1", 1, 1, 1);
    }

    private record Fixture(Member member, Member admin, Topic topic, Concept concept, Question question,
                           KnowledgeChunk chunk) {
    }
}
