package com.example.crackcs.learning.mastery.service;

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
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeCompletionTest {
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
    private EvaluationProcessor evaluationProcessor;

    @Autowired
    private KnowledgeStateService knowledgeStateService;

    @Autowired
    private EvaluationCompletionTransaction completionTransaction;

    @Autowired
    private ConcurrentPort port;

    private static void concurrently(Runnable first, Runnable second) throws Exception {
        concurrently(first, second, 15);
    }

    private static void concurrently(Runnable first, Runnable second, long timeoutSeconds) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        ExecutorCompletionService<Void> completed = new ExecutorCompletionService<>(executorService);
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
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
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
        try {
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
            port.reset();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("동시에 완료하는 두 평가는 AI 재호출 없이 신규 또는 기존 학습 상태에 모두 반영된다")
    void completesConcurrentEvaluationsAtomically(boolean existingState) throws Exception {
        Fixture fixture = fixture();
        if (existingState) {
            completionTransaction.execute(
                    () -> knowledgeStateService.applyInCurrentTransaction(completed(fixture, Verdict.CORRECT)));
        }
        Long first = pending(fixture).getId();
        Long second = pending(fixture).getId();
        concurrently(() -> evaluationProcessor.process(first), () -> evaluationProcessor.process(second));
        KnowledgeState state = knowledgeStateRepository.findByMemberIdAndConceptId(fixture.member().getId(), fixture.concept().getId())
                .orElseThrow();
        assertThat(state.getAttemptCount()).isEqualTo(existingState ? 3 : 2);
        assertThat(appliedEvaluationConceptRepository.count()).isEqualTo(existingState ? 3 : 2);
        assertThat(evaluationRepository.findById(first).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(evaluationRepository.findById(second).orElseThrow().getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(port.calls.get()).isEqualTo(2);
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
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeDocument document = KnowledgeDocument.builder().topic(topic).createdByMember(admin)
                .title("스레드 근거").sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성")
                .content("스레드는 프로세스 자원을 공유하는 실행 단위다.").build();
        document.review(admin);
        document.publish();
        document = knowledgeDocumentRepository.save(document);
        KnowledgeChunk chunk = knowledgeChunkRepository.save(KnowledgeChunk.create(document, 0, 0, document.getContent().length(),
                document.getContent(), "test-v1"));
        return new Fixture(member, admin, topic, concept, question, chunk);
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

    private Evaluation pending(Fixture fixture) {
        return pending(fixture.member(), fixture.question());
    }

    private Evaluation pending(Member member, Question question) {
        Answer answer = answerRepository.save(Answer.builder().member(member).question(question)
                .requestId(UUID.randomUUID().toString()).content("스레드는 실행 단위").build());
        return evaluationRepository.save(Evaluation.builder().answer(answer).build());
    }

    private Long completed(Fixture fixture, Verdict verdict) {
        Long id = pending(fixture).getId();
        complete(id, fixture, verdict);
        return id;
    }

    private void complete(Long id, Fixture fixture, Verdict verdict) {
        transactionTemplate.executeWithoutResult(status -> {
            Evaluation evaluation = evaluationRepository.findById(id).orElseThrow();
            evaluation.completeWithEvidence(result(fixture, verdict),
                    List.of(knowledgeChunkRepository.findById(fixture.chunk().getId()).orElseThrow()));
        });
    }

    private EvaluationResult result(Fixture fixture, Verdict verdict) {
        return new EvaluationResult(verdict, "평가 완료",
                List.of(new ConceptResult(fixture.concept().getId(), verdict, "개념 평가")),
                List.of(), List.of(), List.of(), List.of(fixture.chunk().getId()), "test", "v1", 1, 1, 1);
    }

    @TestConfiguration
    static class PortConfiguration {
        @Bean
        @Primary
        ConcurrentPort concurrentPort() {
            return new ConcurrentPort();
        }
    }

    static class ConcurrentPort implements EvaluationPort {
        final AtomicInteger calls = new AtomicInteger();
        private CyclicBarrier barrier = new CyclicBarrier(2);

        @Override
        public EvaluationResult evaluate(EvaluationRequest request) {
            calls.incrementAndGet();
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
            return new StubEvaluationAdapter("CORRECT").evaluate(request);
        }

        void reset() {
            barrier = new CyclicBarrier(2);
            calls.set(0);
        }
    }

    private record Fixture(Member member, Member admin, Topic topic, Concept concept, Question question,
                           KnowledgeChunk chunk) {
    }
}
