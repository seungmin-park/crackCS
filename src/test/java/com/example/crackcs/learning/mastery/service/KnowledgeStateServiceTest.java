package com.example.crackcs.learning.mastery.service;

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
import com.example.crackcs.evaluation.domain.*;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationCompletionTransaction;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.mastery.domain.AppliedEvaluationConcept;
import com.example.crackcs.learning.mastery.domain.KnowledgeState;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeStateServiceTest {
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
    private KnowledgeStateService service;

    @Autowired
    private EvaluationCompletionTransaction retry;

    private static void concurrently(Runnable first, Runnable second) throws Exception {
        concurrently(first, second, 15);
    }

    private static void concurrently(Runnable first, Runnable second, long timeoutSeconds) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        ExecutorCompletionService<Void> completed = new ExecutorCompletionService<>(pool);
        CountDownLatch start = new CountDownLatch(1);
        Future<Void> firstTask = completed.submit(() -> {
            await(start);
            first.run();
            return null;
        });
        Future<Void> secondTask = completed.submit(() -> {
            await(start);
            second.run();
            return null;
        });
        Throwable taskFailure = null;
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            start.countDown();
            for (int finished = 0; finished < 2; finished++) {
                Future<Void> result = completed.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (result == null) {
                    throw new TimeoutException("Concurrent tasks exceeded the shared deadline");
                }
                result.get();
            }
        } catch (Exception | Error failure) {
            taskFailure = failure;
            throw failure;
        } finally {
            firstTask.cancel(true);
            secondTask.cancel(true);
            pool.shutdownNow();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Concurrent tasks did not terminate; database cleanup may be unsafe");
                }
            } catch (InterruptedException | IllegalStateException cleanupFailure) {
                if (cleanupFailure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (taskFailure != null) {
                    taskFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            } finally {
                if (taskFailure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void await(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @AfterEach
    void cleanUp() {
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
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("명시한 학습 상태와 적용 기록의 유일키 충돌은 여덟 번까지 재시도하고 매번 롤백한다")
    void limitsRetriesForNamedKnowledgeConstraints(boolean duplicateAppliedConcept) {
        Fixture fixture = fixture();
        Long evaluationId = completed(fixture, Verdict.CORRECT);
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> retry.execute(() -> {
            attempts.incrementAndGet();
            service.applyInCurrentTransaction(evaluationId);
            if (duplicateAppliedConcept) {
                EvaluationConcept concept = evaluations.findById(evaluationId).orElseThrow().getConcepts().getFirst();
                appliedConcepts.save(AppliedEvaluationConcept.builder().evaluationConcept(concept).build());
            } else {
                states.save(KnowledgeState.builder().member(fixture.member()).concept(fixture.concept()).build());
            }
        })).isInstanceOf(ConcurrencyFailureException.class).hasCauseInstanceOf(DataIntegrityViolationException.class);

        assertThat(attempts.get()).isEqualTo(8);
        assertThat(states.count()).isZero();
        assertThat(appliedConcepts.count()).isZero();
    }

    @Test
    @DisplayName("다른 트랜잭션이 먼저 상태를 변경하면 오래된 버전을 거부하고 새 트랜잭션으로 재시도한다")
    void retriesARealStaleVersionInANewTransaction() {
        Fixture fixture = fixture();
        retry.execute(() -> service.applyInCurrentTransaction(completed(fixture, Verdict.CORRECT)));
        AtomicInteger attempts = new AtomicInteger();
        LocalDateTime now = LocalDateTime.now();
        retry.execute(() -> {
            KnowledgeState stale = states.findByMemberIdAndConceptId(fixture.member().getId(),
                    fixture.concept().getId()).orElseThrow();
            if (attempts.incrementAndGet() == 1) {
                retry.execute(() -> {
                    KnowledgeState concurrent = states.findByMemberIdAndConceptId(fixture.member().getId(),
                            fixture.concept().getId()).orElseThrow();
                    concurrent.observe(1001L, Verdict.INCORRECT, now);
                });
            }
            stale.observe(1002L, Verdict.PARTIALLY_CORRECT, now.plusSeconds(1));
        });
        assertThat(attempts.get()).isEqualTo(2);
        KnowledgeState saved = states.findByMemberIdAndConceptId(fixture.member().getId(), fixture.concept().getId())
                .orElseThrow();
        assertThat(saved.getAttemptCount()).isEqualTo(3);
        assertThat(saved.getMasteryScore()).isEqualTo(50);
    }

    @Test
    @DisplayName("완료 평가의 선택 개념이 검토 필요이면 해당 개념만 반영에서 제외한다")
    void excludesOnlyOptionalNeedsReviewConcept() {
        Fixture fixture = fixture();
        Concept optional = concept(fixture.topic(), "선택 개념");
        Question question = Question.builder().topic(fixture.topic()).createdByMember(fixture.admin())
                .difficulty(QuestionDifficulty.BASIC)
                .content("필수와 선택 개념").referenceAnswer("답").build();
        question.addConcept(fixture.concept(), new BigDecimal("0.5"), true);
        question.addConcept(optional, new BigDecimal("0.5"), false);
        question.review(fixture.admin());
        question.publish();
        question = questions.save(question);
        Long id = pending(fixture.member(), question).getId();
        transactions.executeWithoutResult(status -> evaluations.findById(id).orElseThrow().completeWithEvidence(
                new EvaluationResult(Verdict.CORRECT, "평가 완료",
                        List.of(new ConceptResult(fixture.concept().getId(), Verdict.CORRECT, "정확"),
                                new ConceptResult(optional.getId(), Verdict.NEEDS_REVIEW, "검토 필요")),
                        List.of(), List.of(), List.of(), List.of(fixture.chunk().getId()), "test", "v1", 1, 1, 1),
                List.of(chunks.findById(fixture.chunk().getId()).orElseThrow())));
        retry.execute(() -> service.applyInCurrentTransaction(id));
        assertThat(states.findByMemberIdAndConceptId(fixture.member().getId(), fixture.concept().getId())).isPresent();
        assertThat(states.findByMemberIdAndConceptId(fixture.member().getId(), optional.getId())).isEmpty();
        assertThat(appliedConcepts.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("완료 평가를 반복 반영해도 관측과 적용 기록은 한 번만 저장한다")
    void appliesExactlyOnce() {
        Fixture fixture = fixture();
        Long id = completed(fixture, Verdict.INCORRECT);
        retry.execute(() -> service.applyInCurrentTransaction(id));
        retry.execute(() -> service.applyInCurrentTransaction(id));
        KnowledgeState state = states.findByMemberIdAndConceptId(fixture.member().getId(), fixture.concept().getId())
                .orElseThrow();
        assertThat(state.getAttemptCount()).isEqualTo(1);
        assertThat(state.getMasteryScore()).isZero();
        assertThat(appliedConcepts.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 평가의 동시 최초 반영은 관측을 둘 다 저장한다")
    void handlesConcurrentCreation() throws Exception {
        Fixture fixture = fixture();
        Long first = completed(fixture, Verdict.CORRECT);
        Long second = completed(fixture, Verdict.INCORRECT);
        concurrently(() -> retry.execute(() -> service.applyInCurrentTransaction(first)),
                () -> retry.execute(() -> service.applyInCurrentTransaction(second)));
        KnowledgeState state = states.findByMemberIdAndConceptId(fixture.member().getId(), fixture.concept().getId())
                .orElseThrow();
        assertThat(state.getAttemptCount()).isEqualTo(2);
        assertThat(state.getMasteryScore()).isCloseTo(100.0 / 3, within(0.0001));
        assertThat(appliedConcepts.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("기존 상태의 동시 갱신과 같은 평가의 재전달에도 관측을 잃거나 중복하지 않는다")
    void handlesConcurrentUpdatesAndDuplicateDelivery() throws Exception {
        Fixture fixture = fixture();
        retry.execute(() -> service.applyInCurrentTransaction(completed(fixture, Verdict.CORRECT)));
        Long second = completed(fixture, Verdict.CORRECT);
        Long third = completed(fixture, Verdict.INCORRECT);
        concurrently(() -> retry.execute(() -> service.applyInCurrentTransaction(second)),
                () -> retry.execute(() -> service.applyInCurrentTransaction(third)));
        concurrently(() -> retry.execute(() -> service.applyInCurrentTransaction(second)),
                () -> retry.execute(() -> service.applyInCurrentTransaction(second)));
        KnowledgeState state = states.findByMemberIdAndConceptId(fixture.member().getId(), fixture.concept().getId())
                .orElseThrow();
        assertThat(state.getAttemptCount()).isEqualTo(3);
        assertThat(state.getMasteryScore()).isEqualTo(50);
        assertThat(appliedConcepts.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("실패와 검토 필요 및 처리 중 평가는 학습 관측에서 제외한다")
    void excludesIneligibleEvaluations() {
        Fixture fixture = fixture();
        Long pending = pending(fixture).getId();
        Long failed = pending(fixture).getId();
        transactions.executeWithoutResult(status -> evaluations.findById(failed).orElseThrow().fail("TEST_FAILURE"));
        Long review = completed(fixture, Verdict.NEEDS_REVIEW);
        retry.execute(() -> service.applyInCurrentTransaction(pending));
        retry.execute(() -> service.applyInCurrentTransaction(failed));
        retry.execute(() -> service.applyInCurrentTransaction(review));
        assertThat(states.count()).isZero();
        assertThat(appliedConcepts.count()).isZero();
    }

    @Test
    @DisplayName("평가 완료 후 트랜잭션이 실패하면 상태와 적용 기록도 함께 롤백한다")
    void rollsBackCompletionAndStateTogether() {
        Fixture fixture = fixture();
        Long id = pending(fixture).getId();
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            complete(id, fixture, Verdict.CORRECT);
            service.applyInCurrentTransaction(id);
            assertThat(states.count()).isEqualTo(1);
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("force rollback");
        assertThat(evaluations.findById(id).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(states.count()).isZero();
        assertThat(appliedConcepts.count()).isZero();
    }

    @Test
    @DisplayName("동시 작업 하나가 실패하면 대기 중인 다른 작업을 취소하고 종료한다")
    void cancelsOtherTaskOnFailure() {
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();

        assertThatThrownBy(() -> concurrently(() -> {
            await(waiting);
            throw new IllegalStateException("task failed");
        }, () -> {
            waiting.countDown();
            try {
                // 이전 구현에서도 테스트가 무한 대기하지 않도록 안전장치 설정
                new CountDownLatch(1).await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
        })).hasCauseInstanceOf(IllegalStateException.class);

        assertThat(interrupted).isTrue();
    }

    @Test
    @DisplayName("동시 작업의 제한 시간이 지나면 남은 작업을 취소한다")
    void cancelsTasksOnTimeout() {
        AtomicBoolean interrupted = new AtomicBoolean();

        assertThatThrownBy(() -> concurrently(() -> {
            try {
                new CountDownLatch(1).await(3, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
        }, () -> {
        }, 1)).isInstanceOf(TimeoutException.class);

        assertThat(interrupted).isTrue();
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
