package com.example.crackcs.learning.mastery.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.adapter.StubEvaluationAdapter;
import com.example.crackcs.evaluation.domain.*;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationCompletionTransaction;
import com.example.crackcs.evaluation.service.EvaluationOperationLogger;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "crackcs.evaluation.retry-base-delay=0ms",
        "spring.datasource.hikari.connection-init-sql=SET LOCK_TIMEOUT 25"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KnowledgeCompletionFailureTest {
    private final Logger operationLogger =
            (Logger) LoggerFactory.getLogger(EvaluationOperationLogger.class);
    private final ListAppender<ILoggingEvent> operationAppender = new ListAppender<>();

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    private KnowledgeStateRepository knowledgeStateRepository;

    @Autowired
    private AppliedEvaluationConceptRepository appliedEvaluationConceptRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EvaluationProcessor evaluationProcessor;

    @Autowired
    private EvaluationCompletionTransaction completionTransaction;

    @Autowired
    private KnowledgeStateService knowledgeStateService;

    @Autowired
    private ControlledPort port;

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

    @AfterEach
    void cleanUp() {
        try {
            jdbcTemplate.execute("alter table knowledge_state drop constraint if exists test_reject_knowledge_observation");
            appliedEvaluationConceptRepository.deleteAllInBatch();
            knowledgeStateRepository.deleteAllInBatch();
            evaluationRepository.deleteAll();
            answerRepository.deleteAllInBatch();
            questionRepository.deleteAll();
            knowledgeChunkRepository.deleteAllInBatch();
            knowledgeDocumentRepository.deleteAllInBatch();
            conceptRepository.deleteAllInBatch();
            topicRepository.deleteAllInBatch();
            memberRepository.deleteAllInBatch();
        } finally {
            operationLogger.detachAppender(operationAppender);
            port.reset();
        }
    }

    @Test
    @DisplayName("영구 저장 오류는 평가를 실패로 확정하고 AI를 다시 호출하지 않는다")
    void failsPermanentStorageErrorsWithoutAnotherProviderCall() {
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        knowledgeChunk(topic, admin);
        Long evaluationId = savePendingEvaluation(learner, question).getId();
        jdbcTemplate.execute(
                "alter table knowledge_state add constraint test_reject_knowledge_observation check (attempt_count = 0)");

        evaluationProcessor.process(evaluationId);
        evaluationProcessor.process(evaluationId);

        Evaluation savedEvaluation = evaluationRepository.findById(evaluationId).orElseThrow();
        assertThat(savedEvaluation.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(savedEvaluation.getFailureReason()).isEqualTo("PERSISTENCE_ERROR");
        assertThat(savedEvaluation.getAttemptCount()).isEqualTo(1);
        assertThat(savedEvaluation.getLeaseOwner()).isNull();
        assertThat(port.calls.get()).isEqualTo(1);
        assertThat(knowledgeStateRepository.count()).isZero();
        assertThat(appliedEvaluationConceptRepository.count()).isZero();
        Evaluation details = evaluationRepository.findByAnswerId(savedEvaluation.getAnswer().getId()).orElseThrow();
        assertThat(details.getConcepts()).isEmpty();
        assertThat(details.getEvidence()).isEmpty();
    }

    @Test
    @DisplayName("완료 잠금 재시도를 소진하면 저장 충돌 사유로 예약하고 잠금 해제 후 완료한다")
    void labelsExhaustedLockConflictsAsPersistenceConflicts() throws Exception {
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeChunk chunk = knowledgeChunk(topic, admin);
        Long initialId = saveCompletedEvaluation(learner, question, concept, chunk, Verdict.CORRECT);
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(initialId));
        Long evaluationId = savePendingEvaluation(learner, question).getId();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> holder = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                jdbcTemplate.queryForObject("select id from knowledge_state where member_id = ? and concept_id = ? for update",
                        Long.class, learner.getId(), concept.getId());
                locked.countDown();
                await(release);
            }));
            try {
                assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
                evaluationProcessor.process(evaluationId);

                Evaluation scheduled = evaluationRepository.findById(evaluationId).orElseThrow();
                assertThat(scheduled.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
                assertThat(scheduled.getFailureReason()).isEqualTo("PERSISTENCE_CONFLICT");
                assertThat(scheduled.getAttemptCount()).isEqualTo(1);
                assertThat(port.calls.get()).isEqualTo(1);
                assertThat(knowledgeStateRepository.findByMemberIdAndConceptId(learner.getId(), concept.getId())
                        .orElseThrow().getAttemptCount()).isEqualTo(1);
                assertThat(appliedEvaluationConceptRepository.count()).isEqualTo(1);
            } finally {
                release.countDown();
            }
            holder.get(5, TimeUnit.SECONDS);
        }

        evaluationProcessor.process(evaluationId);

        assertThat(evaluationRepository.findById(evaluationId).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(port.calls.get()).isEqualTo(2);
        assertThat(knowledgeStateRepository.findByMemberIdAndConceptId(learner.getId(), concept.getId())
                .orElseThrow().getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("AI 제공자 오류는 기존 재시도 정책과 안전한 오류 사유를 유지한다")
    void preservesProviderFailureRetries() {
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        knowledgeChunk(topic, admin);
        Long evaluationId = savePendingEvaluation(learner, question).getId();
        port.unavailable = true;

        evaluationProcessor.process(evaluationId);

        Evaluation retrying = evaluationRepository.findById(evaluationId).orElseThrow();
        assertThat(retrying.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(retrying.getFailureReason()).isEqualTo("PROVIDER_ERROR");
        evaluationProcessor.process(evaluationId);
        evaluationProcessor.process(evaluationId);

        Evaluation failed = evaluationRepository.findById(evaluationId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("PROVIDER_ERROR");
        assertThat(port.calls.get()).isEqualTo(3);
        assertThat(knowledgeStateRepository.count()).isZero();
        assertThat(appliedEvaluationConceptRepository.count()).isZero();
    }

    @Test
    @DisplayName("잘못된 provider 결과는 부분 평가를 남기지 않고 원문을 보존한다")
    void rejectsInvalidProviderResultWithoutPartialState() {
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        knowledgeChunk(topic, admin);
        Evaluation pending = savePendingEvaluation(learner, question);
        Long evaluationId = pending.getId();
        Long answerId = pending.getAnswer().getId();
        port.invalidResult = true;

        evaluationProcessor.process(evaluationId);
        evaluationProcessor.process(evaluationId);
        evaluationProcessor.process(evaluationId);

        Evaluation failed = evaluationRepository.findById(evaluationId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("INVALID_RESULT");
        assertThat(answerRepository.findById(answerId).orElseThrow().getContent()).isEqualTo("스레드는 실행 단위");
        assertThat(evaluationRepository.findByAnswerId(answerId).orElseThrow().getConcepts()).isEmpty();
        assertThat(evaluationRepository.findByAnswerId(answerId).orElseThrow().getEvidence()).isEmpty();
        assertThat(knowledgeStateRepository.count()).isZero();
        assertThat(appliedEvaluationConceptRepository.count()).isZero();
        assertThat(port.calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("완료 전에 lease를 잃으면 성공 로그를 남기지 않는다")
    void doesNotLogCompletionAfterLosingLease() {
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        knowledgeChunk(topic, admin);
        Long evaluationId = savePendingEvaluation(learner, question).getId();
        operationAppender.start();
        operationLogger.addAppender(operationAppender);
        port.beforeEvaluate = () -> jdbcTemplate.update(
                "update evaluation set lease_owner = 'another-worker' where id = ?",
                evaluationId
        );

        evaluationProcessor.process(evaluationId);

        Evaluation savedEvaluation = evaluationRepository.findById(evaluationId).orElseThrow();
        String messages = operationAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertThat(savedEvaluation.getStatus()).isEqualTo(EvaluationStatus.PROCESSING);
        assertThat(messages).doesNotContain("event=evaluation_completed");
    }

    @Test
    @DisplayName("실패 결정 전에 lease를 잃으면 실패 로그를 남기지 않는다")
    void doesNotLogFailureAfterLosingLease() {
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        knowledgeChunk(topic, admin);
        Long evaluationId = savePendingEvaluation(learner, question).getId();
        operationAppender.start();
        operationLogger.addAppender(operationAppender);
        port.beforeEvaluate = () -> jdbcTemplate.update(
                "update evaluation set lease_owner = 'another-worker' where id = ?",
                evaluationId
        );
        port.unavailable = true;

        evaluationProcessor.process(evaluationId);

        Evaluation savedEvaluation = evaluationRepository.findById(evaluationId).orElseThrow();
        String messages = operationAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertThat(savedEvaluation.getStatus()).isEqualTo(EvaluationStatus.PROCESSING);
        assertThat(messages).doesNotContain("event=evaluation_failed");
    }

    private KnowledgeChunk knowledgeChunk(Topic topic, Member admin) {
        KnowledgeDocument document = KnowledgeDocument.builder().topic(topic).createdByMember(admin)
                .title("스레드 근거").sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성")
                .content("스레드는 프로세스 자원을 공유하는 실행 단위다.").build();
        document.review(admin);
        document.publish();
        document = knowledgeDocumentRepository.save(document);
        return knowledgeChunkRepository.save(KnowledgeChunk.create(document, 0, 0, document.getContent().length(),
                document.getContent(), "test-v1"));
    }

    private Concept concept(Topic topic, String name) {
        return conceptRepository.save(Concept.builder().topic(topic).code(UUID.randomUUID().toString()).name(name).build());
    }

    private Question question(Member admin, Topic topic, Concept concept, String content) {
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content(content).referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.replaceConcepts(List.of(
                new QuestionConceptAssignment(concept, BigDecimal.ONE, true)
        ));
        question.review(admin);
        question.publish();
        return questionRepository.save(question);
    }

    private Evaluation savePendingEvaluation(Member learner, Question question) {
        Answer answer = answerRepository.save(Answer.builder().member(learner).question(question)
                .idempotencyKey(UUID.randomUUID().toString()).content("스레드는 실행 단위").build());
        return evaluationRepository.save(Evaluation.builder().answer(answer).build());
    }

    private Long saveCompletedEvaluation(Member learner, Question question, Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        Long id = savePendingEvaluation(learner, question).getId();
        completeEvaluation(id, concept, chunk, verdict);
        return id;
    }

    private void completeEvaluation(Long id, Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        transactionTemplate.executeWithoutResult(status -> {
            Evaluation evaluation = evaluationRepository.findById(id).orElseThrow();
            evaluation.completeWithEvidence(evaluationResult(concept, chunk, verdict),
                    List.of(knowledgeChunkRepository.findById(chunk.getId()).orElseThrow()));
        });
    }

    private EvaluationResult evaluationResult(Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        return new EvaluationResult(verdict, "평가 완료",
                List.of(new ConceptResult(concept.getId(), verdict, "개념 평가")),
                List.of(), List.of(), List.of(), List.of(chunk.getId()), "test", "v1", 1, 1, 1);
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
        private boolean invalidResult;
        private Runnable beforeEvaluate = () -> {
        };

        @Override
        public EvaluationResult evaluate(EvaluationRequest request) {
            calls.incrementAndGet();
            beforeEvaluate.run();
            if (unavailable) {
                throw new IllegalStateException("provider detail must not escape");
            }
            if (invalidResult) {
                throw new IllegalArgumentException("invalid provider result detail must not escape");
            }
            return new StubEvaluationAdapter("CORRECT").evaluate(request);
        }

        void reset() {
            calls.set(0);
            unavailable = false;
            invalidResult = false;
            beforeEvaluate = () -> {
            };
        }
    }

}
