package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkService;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.exception.AnswerNotFoundException;
import com.example.crackcs.exception.EvaluationTimeoutException;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.answer.service.AnswerService;
import com.example.crackcs.learning.answer.service.result.AnswerResult;
import com.example.crackcs.learning.followup.adapter.StubFollowUpQuestionAdapter;
import com.example.crackcs.learning.followup.domain.FollowUpGeneration;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.domain.FollowUpResult;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;
import com.example.crackcs.learning.followup.port.FollowUpQuestionGenerator;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import com.example.crackcs.learning.followup.repository.FollowUpGenerationRepository;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.learning.recommendation.service.RecommendationService;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"crackcs.evaluation.worker-enabled=false", "crackcs.followup.worker-enabled=false",
        "crackcs.followup.retry-delay=0ms"})
@ActiveProfiles("test")
class FollowUpQuestionServiceTest {
    @Autowired
    private CompletionFailureTransactions completionFailures;

    @Autowired
    private FollowUpGenerationRepository followUpGenerationRepository;
    @Autowired
    private ControlledPort port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private FollowUpQuestionService followUpQuestionService;
    @Autowired
    private FollowUpQuestionProcessor followUpQuestionProcessor;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private EvaluationProcessor evaluationProcessor;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private EvaluationRepository evaluationRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private ConceptRepository conceptRepository;
    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Autowired
    private KnowledgeChunkService knowledgeChunkService;
    @Autowired
    private AppliedEvaluationConceptRepository appliedEvaluationConceptRepository;
    @Autowired
    private KnowledgeStateRepository knowledgeStateRepository;
    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("일시적 완료 저장 충돌은 AI 재호출 없이 새 트랜잭션에서 복구한다")
    void retriesCompletionWithoutRegenerating() {
        AnswerResult source = source();
        evaluate(source.answerId());
        AtomicInteger calls = new AtomicInteger();
        port.behavior = request -> {
            calls.incrementAndGet();
            return new StubFollowUpQuestionAdapter().generate(request);
        };
        completionFailures.failNextCompletions(1);

        followUpQuestionProcessor.process(source.answerId());

        assertThat(followUpGenerationRepository.findByAnswerId(source.answerId()).orElseThrow().getStatus()).isEqualTo(FollowUpStatus.READY);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(questionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("완료 저장 충돌이 제한 횟수를 넘으면 질문을 남기지 않고 실패로 종료한다")
    void boundsCompletionRetries() {
        AnswerResult source = source();
        evaluate(source.answerId());
        completionFailures.failNextCompletions(10);

        followUpQuestionProcessor.process(source.answerId());

        FollowUpGeneration job = followUpGenerationRepository.findByAnswerId(source.answerId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(FollowUpStatus.FAILED);
        assertThat(job.getReason()).isEqualTo(FollowUpReason.PERSISTENCE_ERROR);
        assertThat(completionFailures.failureCount).isEqualTo(3);
        assertThat(questionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("외부 트랜잭션에서는 후속 생성을 시작하지 않는다")
    void rejectsCallerTransactionBeforeClaim() {
        AnswerResult source = source();
        evaluate(source.answerId());

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> followUpQuestionProcessor.process(source.answerId())))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(followUpGenerationRepository.count()).isZero();
        assertThat(questionRepository.count()).isEqualTo(1);
    }

    @AfterEach
    void tearDown() {
        completionFailures.failNextCompletions(0);
        port.reset();
        jdbcTemplate.execute("alter table follow_up_generation drop constraint if exists test_reject_ready");
        followUpGenerationRepository.deleteAll();
        appliedEvaluationConceptRepository.deleteAllInBatch();
        knowledgeStateRepository.deleteAllInBatch();
        evaluationRepository.deleteAll();
        transactionTemplate.executeWithoutResult(status -> {
            answerRepository.findAll().stream().filter(answer -> answer.getQuestion().getType() == QuestionType.FOLLOW_UP)
                    .forEach(answerRepository::delete);
            answerRepository.flush();
            questionRepository.findAll().stream().filter(question -> question.getType() == QuestionType.FOLLOW_UP)
                    .forEach(questionRepository::delete);
        });
        answerRepository.deleteAllInBatch();
        questionRepository.deleteAll();
        knowledgeChunkRepository.deleteAllInBatch();
        knowledgeDocumentRepository.deleteAllInBatch();
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("평가 처리 중에는 생성 작업을 만들지 않고 대기 상태를 유지한다")
    void waitsForProcessingEvaluation() {
        AnswerResult source = source();
        transactionTemplate.executeWithoutResult(status -> evaluationRepository.findByAnswerId(source.answerId()).orElseThrow()
                .claim("evaluation-worker", LocalDateTime.now(), Duration.ofMinutes(1)));
        followUpQuestionProcessor.process(source.answerId());
        assertThat(followUpQuestionService.findByAnswerId(owner(source), source.answerId()).status()).isEqualTo(FollowUpStatus.PENDING);
        assertThat(followUpGenerationRepository.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = EvaluationStatus.class, names = {"FAILED", "NEEDS_REVIEW"})
    @DisplayName("실패하거나 검토가 필요한 평가는 후속 생성 대상에서 제외한다")
    void excludesIneligibleEvaluations(EvaluationStatus evaluationStatus) {
        AnswerResult source = source();
        transactionTemplate.executeWithoutResult(status -> {
            if (evaluationStatus == EvaluationStatus.FAILED) {
                evaluationRepository.findByAnswerId(source.answerId()).orElseThrow().fail("PROVIDER_ERROR");
            } else {
                evaluationRepository.findByAnswerId(source.answerId()).orElseThrow().requireReview("EVIDENCE_NOT_FOUND");
            }
        });
        followUpQuestionProcessor.process(source.answerId());
        assertThat(followUpQuestionService.findByAnswerId(owner(source), source.answerId()).reason()).isEqualTo(FollowUpReason.EVALUATION_NOT_ELIGIBLE);
        assertThat(followUpGenerationRepository.count()).isZero();
    }

    @Test
    @DisplayName("관리자 문제 목록과 상세에도 개인 후속 질문을 노출하지 않는다")
    void hidesFollowUpsFromAdminCatalog() {
        AnswerResult source = source();
        evaluate(source.answerId());
        followUpQuestionProcessor.process(source.answerId());
        Long id = followUpQuestionService.findByAnswerId(owner(source), source.answerId()).question().id();
        assertThat(questionRepository.findNormalWithConceptsById(id)).isEmpty();
        assertThat(questionRepository.findNormalByConditions(null, null, null, null, PageRequest.of(0, 20))
                .getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에 같은 답변을 처리해도 AI 호출과 후속 질문은 하나이다")
    void concurrentProcessingClaimsOnce() throws Exception {
        AnswerResult source = source();
        evaluate(source.answerId());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        port.behavior = request -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            calls.incrementAndGet();
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test provider wait expired");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
            return new StubFollowUpQuestionAdapter().generate(request);
        };
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executorService.submit(() -> followUpQuestionProcessor.process(source.answerId()));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                Future<?> second = executorService.submit(() -> followUpQuestionProcessor.process(source.answerId()));
                second.get(5, TimeUnit.SECONDS);
                assertThat(followUpQuestionService.findByAnswerId(owner(source), source.answerId()).status()).isEqualTo(FollowUpStatus.PROCESSING);
            } finally {
                release.countDown();
            }
            first.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executorService.shutdownNow();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
        assertThat(calls.get()).isEqualTo(1);
        assertThat(followUpQuestionService.findByAnswerId(owner(source), source.answerId()).status()).isEqualTo(FollowUpStatus.READY);
        assertThat(followUpGenerationRepository.count()).isEqualTo(1);
        assertThat(questionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("타임아웃 세 번 이후 실패해도 기존 평가와 숙련도는 유지한다")
    void timeoutPreservesEvaluation() {
        AnswerResult source = source();
        evaluate(source.answerId());
        port.behavior = request -> {
            throw new EvaluationTimeoutException();
        };
        for (int attempt = 0; attempt < 4; attempt++) {
            followUpQuestionProcessor.process(source.answerId());
        }
        FollowUpGeneration job = followUpGenerationRepository.findByAnswerId(source.answerId()).orElseThrow();
        assertThat(job.getAttemptCount()).isEqualTo(3);
        assertThat(job.getStatus()).isEqualTo(FollowUpStatus.FAILED);
        assertThat(job.getReason()).isEqualTo(FollowUpReason.PROVIDER_TIMEOUT);
        assertThat(evaluationRepository.findByAnswerId(source.answerId()).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(appliedEvaluationConceptRepository.count()).isEqualTo(1);
        assertThat(questionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("승인하지 않은 근거 출력은 재시도 없이 거부한다")
    void rejectsUnapprovedEvidence() {
        AnswerResult source = source();
        evaluate(source.answerId());
        port.behavior = request -> new FollowUpResult("질문", "정답", request.conceptId(), List.of(Long.MAX_VALUE),
                "test", "follow-up-v1", 0, 0, 0);
        followUpQuestionProcessor.process(source.answerId());
        FollowUpGeneration job = followUpGenerationRepository.findByAnswerId(source.answerId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(FollowUpStatus.FAILED);
        assertThat(job.getReason()).isEqualTo(FollowUpReason.INVALID_RESULT);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(questionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("AI 응답을 기다리는 동안 근거가 폐기되면 저장하지 않는다")
    void rechecksEvidenceAtCompletion() {
        AnswerResult source = source();
        evaluate(source.answerId());
        port.behavior = request -> {
            transactionTemplate.executeWithoutResult(status -> knowledgeDocumentRepository.findAll().forEach(KnowledgeDocument::retire));
            return new StubFollowUpQuestionAdapter().generate(request);
        };
        followUpQuestionProcessor.process(source.answerId());
        assertThat(followUpGenerationRepository.findByAnswerId(source.answerId()).orElseThrow().getStatus()).isEqualTo(FollowUpStatus.UNAVAILABLE);
        assertThat(questionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("작업 완료 저장이 실패하면 후속 질문 삽입도 함께 롤백한다")
    void completionFailureRollsBackQuestion() {
        AnswerResult source = source();
        evaluate(source.answerId());
        jdbcTemplate.execute("alter table follow_up_generation add constraint test_reject_ready check (status <> 'READY')");
        followUpQuestionProcessor.process(source.answerId());
        assertThat(followUpGenerationRepository.findByAnswerId(source.answerId()).orElseThrow().getReason()).isEqualTo(FollowUpReason.PERSISTENCE_ERROR);
        assertThat(questionRepository.count()).isEqualTo(1);
        assertThat(evaluationRepository.findByAnswerId(source.answerId()).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
    }

    @Test
    @DisplayName("Worker는 작업 생성이 누락된 완료 평가를 다시 찾아 처리한다")
    void pollingRecoversMissingJob() {
        AnswerResult source = source();
        evaluate(source.answerId());
        assertThat(followUpGenerationRepository.count()).isZero();
        new FollowUpWorker(followUpGenerationRepository, followUpQuestionProcessor, port).processPending();
        assertThat(followUpQuestionService.findByAnswerId(owner(source), source.answerId()).status()).isEqualTo(FollowUpStatus.READY);
    }

    @Test
    @DisplayName("같은 원본 답변의 두 번째 후속 질문은 DB 유일 제약으로 막는다")
    void databaseRejectsDuplicateQuestion() {
        AnswerResult source = source();
        evaluate(source.answerId());
        followUpQuestionProcessor.process(source.answerId());
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            Answer answer = answerRepository.findById(source.answerId()).orElseThrow();
            questionRepository.save(Question.followUpBuilder().sourceAnswer(answer)
                    .concept(answer.getQuestion().getQuestionConcepts().iterator().next().getConcept())
                    .content("중복").referenceAnswer("정답").build());
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(questionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("원본 답변이 없는 NORMAL 문제 여러 개는 nullable 유일 제약을 통과한다")
    void databaseAllowsMultipleNormalQuestions() {
        AnswerResult source = source();
        transactionTemplate.executeWithoutResult(status -> {
            Question original = questionRepository.findById(source.questionId()).orElseThrow();
            questionRepository.save(Question.builder().topic(original.getTopic()).createdByMember(original.getCreatedByMember())
                    .difficulty(QuestionDifficulty.BASIC).content("두 번째 기본").referenceAnswer("정답").build());
        });
        assertThat(questionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("DB에서 읽은 원본 답변에서도 후속 질문의 개념을 연결한다")
    void createsFromPersistedSource() {
        AnswerResult source = source();
        transactionTemplate.executeWithoutResult(status -> {
            Answer answer = answerRepository.findById(source.answerId()).orElseThrow();
            Question question = Question.followUpBuilder().sourceAnswer(answer)
                    .concept(answer.getQuestion().getQuestionConcepts().iterator().next().getConcept())
                    .content("질문").referenceAnswer("정답").build();
            questionRepository.save(question);
        });
        assertThat(questionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("평가 대기 중 조회는 후속 질문을 만들지 않고 대기 상태를 반환한다")
    void pendingReadHasNoGenerationSideEffect() {
        AnswerResult answer = source();
        assertThat(followUpQuestionService.findByAnswerId(owner(answer), answer.answerId()).status()).isEqualTo(FollowUpStatus.PENDING);
        assertThat(questionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 답변과 타인의 답변은 같은 조회 실패로 숨긴다")
    void hidesUnownedAnswers() {
        AnswerResult answer = source();
        assertThatThrownBy(() -> followUpQuestionService.findByAnswerId(-1L, answer.answerId())).isInstanceOf(AnswerNotFoundException.class);
        assertThatThrownBy(() -> followUpQuestionService.findByAnswerId(owner(answer), -1L)).isInstanceOf(AnswerNotFoundException.class);
    }

    @Test
    @DisplayName("기본 평가에서 후속 답변 평가까지 이어지고 재조회와 재처리는 같은 질문을 유지한다")
    void completesLearningLoopOnce() {
        AnswerResult source = source();
        evaluate(source.answerId());
        followUpQuestionProcessor.process(source.answerId());
        FollowUpQuestionResult result = followUpQuestionService.findByAnswerId(owner(source), source.answerId());
        assertThat(result.status()).isEqualTo(FollowUpStatus.READY);
        Long followUpId = result.question().id();
        followUpQuestionProcessor.process(source.answerId());
        assertThat(followUpQuestionService.findByAnswerId(owner(source), source.answerId()).question().id()).isEqualTo(followUpId);
        assertThat(questionRepository.findPublishedNormalById(followUpId)).isEmpty();
        assertThat(questionRepository.findPublishedNormalQuestions(null, null, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
        Member other = memberRepository.save(Member.builder().nickname("다른 회원").build());
        assertThatThrownBy(() -> answerService.submit(other.getId(), followUpId, UUID.randomUUID().toString(), "답변"))
                .isInstanceOf(QuestionNotFoundException.class);
        AnswerResult followUp = answerService.submit(owner(source), followUpId, UUID.randomUUID().toString(),
                "스레드는 프로세스 자원을 공유하는 실행 단위입니다.");
        evaluate(followUp.answerId());
        assertThat(evaluationRepository.findByAnswerId(followUp.answerId()).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(appliedEvaluationConceptRepository.count()).isEqualTo(2);
        assertThat(knowledgeStateRepository.findAll()).singleElement().satisfies(state -> assertThat(state.getAttemptCount()).isEqualTo(2));
        assertThat(recommendationService.recommendation(owner(source)).questionId()).isEqualTo(source.questionId());
        assertThat(followUpQuestionService.findByAnswerId(owner(source), followUp.answerId()).reason()).isEqualTo(FollowUpReason.FOLLOW_UP_LIMIT);
        followUpQuestionProcessor.process(followUp.answerId());
        assertThat(questionRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("근거 문서 폐기 후에는 후속 생성을 사용할 수 없다")
    void rejectsRetiredEvidence() {
        AnswerResult source = source();
        evaluate(source.answerId());
        transactionTemplate.executeWithoutResult(status -> knowledgeDocumentRepository.findAll().forEach(KnowledgeDocument::retire));
        followUpQuestionProcessor.process(source.answerId());
        assertThat(followUpQuestionService.findByAnswerId(owner(source), source.answerId()).reason()).isEqualTo(FollowUpReason.CONTENT_UNAVAILABLE);
        assertThat(evaluationRepository.findByAnswerId(source.answerId()).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
    }

    private void evaluate(Long answerId) {
        evaluationProcessor.process(evaluationRepository.findByAnswerId(answerId).orElseThrow().getId());
    }

    private Long owner(AnswerResult answer) {
        return answerRepository.findById(answer.answerId()).orElseThrow().getMember().getId();
    }

    private AnswerResult source() {
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Topic topic = topicRepository.save(Topic.builder().code("OS").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("THREAD").name("스레드").build());
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content("스레드를 설명하세요").referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
        question.review(admin);
        question.publish();
        questionRepository.save(question);
        KnowledgeDocument document = KnowledgeDocument.builder().topic(topic).createdByMember(admin)
                .title("스레드 근거").sourceType(KnowledgeSourceType.INTERNAL_SUMMARY).technologyVersion("general")
                .licenseNote("독립 작성").content("스레드는 프로세스 자원을 공유하는 실행 단위다.").build();
        document.review(admin);
        document.publish();
        knowledgeDocumentRepository.save(document);
        knowledgeChunkService.generateChunks(document.getId());
        return answerService.submit(member.getId(), question.getId(), UUID.randomUUID().toString(),
                "스레드는 프로세스 자원을 공유하는 실행 단위입니다.");
    }

    @TestConfiguration
    static class PortConfiguration {
        @Bean
        @Primary
        CompletionFailureTransactions completionFailureTransactions(PlatformTransactionManager manager,
                                                                    JdbcTemplate jdbcTemplate, QuestionRepository questionRepository) {
            return new CompletionFailureTransactions(manager, jdbcTemplate, questionRepository);
        }

        @Bean
        @Primary
        ControlledPort controlledPort() {
            return new ControlledPort();
        }
    }

    // 실제 DB 쓰기와 롤백은 유지하고, 완료 시점의 일시적인 DB 실패만 제어한다.
    static class CompletionFailureTransactions extends TransactionTemplate {
        private final JdbcTemplate jdbcTemplate;
        private final QuestionRepository questionRepository;
        private int remainingFailures;
        private int failureCount;

        CompletionFailureTransactions(PlatformTransactionManager manager, JdbcTemplate jdbcTemplate,
                                      QuestionRepository questionRepository) {
            super(manager);
            this.jdbcTemplate = jdbcTemplate;
            this.questionRepository = questionRepository;
        }

        void failNextCompletions(int count) {
            remainingFailures = count;
            failureCount = 0;
        }

        @Override
        public void executeWithoutResult(Consumer<TransactionStatus> action) {
            super.executeWithoutResult(status -> {
                action.accept(status);
                if (remainingFailures > 0) {
                    questionRepository.flush();
                    Integer ready = jdbcTemplate.queryForObject(
                            "select count(*) from follow_up_generation where status = 'READY'", Integer.class);
                    if (ready != null && ready > 0) {
                        remainingFailures--;
                        failureCount++;
                        throw new PessimisticLockingFailureException("controlled completion conflict");
                    }
                }
            });
        }
    }

    static class ControlledPort implements FollowUpQuestionGenerator {
        private Function<FollowUpRequest, FollowUpResult> behavior = new StubFollowUpQuestionAdapter()::generate;

        public FollowUpResult generate(FollowUpRequest request) {
            return behavior.apply(request);
        }

        void reset() {
            behavior = new StubFollowUpQuestionAdapter()::generate;
        }
    }
}
