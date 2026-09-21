package com.example.crackcs.evaluation.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.port.EvaluatedConceptApplicationPort;
import com.example.crackcs.evaluation.port.EvaluationConceptInput;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.learning.answer.domain.Answer;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DefaultEvaluationProcessor implements EvaluationProcessor {

    private final EvaluationRepository evaluationRepository;
    private final TransactionTemplate transactionTemplate;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final EvaluationAttemptExecutor evaluationAttemptExecutor;
    private final EvaluatedConceptApplicationPort evaluatedConceptApplicationPort;
    private final EvaluationCompletionTransaction evaluationCompletionTransaction;
    private final EvaluationOperationLogger evaluationOperationLogger;
    private final String workerId = UUID.randomUUID().toString();
    private final Object[] localLocks = IntStream.range(0, 64).mapToObj(ignored -> new Object()).toArray();
    @Value("${crackcs.evaluation.lease-duration:1m}")
    private Duration leaseDuration;
    @Value("${crackcs.evaluation.retry-base-delay:1s}")
    private Duration retryBaseDelay;

    @Override
    @Timed(value = "crackcs.evaluation.process", description = "Evaluation processing time")
    public void process(Long evaluationId) {
        Object localLock = localLocks[Math.floorMod(evaluationId.hashCode(), localLocks.length)];
        synchronized (localLock) {
            processWithLocalLock(evaluationId);
        }
    }

    private void processWithLocalLock(Long evaluationId) {
        Optional<ClaimedEvaluationWork> claimed = Objects.requireNonNull(
                transactionTemplate.execute(status -> claim(evaluationId)),
                "claim transaction must return an Optional");
        if (claimed.isEmpty()) {
            return;
        }
        ClaimedEvaluationWork work = claimed.orElseThrow();
        applyAttempt(work, evaluationAttemptExecutor.execute(work));
    }

    private void applyAttempt(ClaimedEvaluationWork work, EvaluationAttempt attempt) {
        switch (attempt) {
            case EvaluationAttempt.Completed completed -> complete(work, completed);
            case EvaluationAttempt.ReviewRequired review -> applyReview(work, review.reason());
            case EvaluationAttempt.RetryRequired retry -> applyRetry(work, retry.reason());
        }
    }

    private void applyReview(ClaimedEvaluationWork work, String reason) {
        if (requireReviewIfOwned(work.evaluationId(), reason)) {
            logFailure(work, reason);
        }
    }

    private void applyRetry(ClaimedEvaluationWork work, String reason) {
        if (retryOrFail(work.evaluationId(), work.attemptCount(), reason)) {
            logFailure(work, reason);
        }
    }

    private void complete(ClaimedEvaluationWork work, EvaluationAttempt.Completed completed) {
        try {
            // A storage conflict retries completion using this same provider result in a fresh transaction.
            AtomicBoolean completionApplied = new AtomicBoolean();
            evaluationCompletionTransaction.execute(() -> completionApplied.set(
                    completeIfOwned(work.evaluationId(), completed.result(), completed.evidenceChunkIds())));
            if (completionApplied.get()) {
                evaluationOperationLogger.evaluationCompleted(
                        work.evaluationId(),
                        work.answerId(),
                        work.memberId(),
                        completed.result().modelName(),
                        completed.result().evaluatorVersion()
                );
            }
        } catch (DataIntegrityViolationException permanentStorageFailure) {
            if (failIfOwned(work.evaluationId(), "PERSISTENCE_ERROR")) {
                logFailure(work, "PERSISTENCE_ERROR");
            }
        } catch (ConcurrencyFailureException exhaustedConflict) {
            if (retryOrFail(work.evaluationId(), work.attemptCount(), "PERSISTENCE_CONFLICT")) {
                logFailure(work, "PERSISTENCE_CONFLICT");
            }
        } catch (IllegalArgumentException invalidResult) {
            if (retryOrFail(work.evaluationId(), work.attemptCount(), "INVALID_RESULT")) {
                logFailure(work, "INVALID_RESULT");
            }
        }
    }

    private void logFailure(ClaimedEvaluationWork work, String failureCode) {
        evaluationOperationLogger.evaluationFailed(
                work.evaluationId(),
                work.answerId(),
                work.memberId(),
                failureCode
        );
    }

    private Optional<ClaimedEvaluationWork> claim(Long evaluationId) {
        Optional<Evaluation> found = evaluationRepository.findLockedById(evaluationId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Evaluation evaluation = found.orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        if (!evaluation.claim(workerId, now, leaseDuration)) {
            return Optional.empty();
        }
        return Optional.of(toClaimedWork(evaluation));
    }

    private ClaimedEvaluationWork toClaimedWork(Evaluation evaluation) {
        Answer answer = evaluation.getAnswer();
        Question question = answer.getQuestion();
        List<EvaluationConceptInput> concepts = question.getQuestionConcepts().stream()
                .map(qc -> new EvaluationConceptInput(qc.getConcept().getId(), qc.getConcept().getName(),
                        qc.isRequired()))
                .toList();
        return new ClaimedEvaluationWork(
                evaluation.getId(),
                answer.getId(), answer.getMember().getId(),
                question.getTopic().getId(), question.getContent(), question.getReferenceAnswer(),
                answer.getContent(), concepts, evaluation.getAttemptCount()
        );
    }

    private boolean completeIfOwned(
            Long evaluationId,
            EvaluationResult result,
            List<Long> providedChunkIds
    ) {
        List<KnowledgeChunk> providedChunks = knowledgeChunkRepository.findAllById(providedChunkIds);
        return evaluationRepository.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .map(evaluation -> {
                    evaluation.completeWithEvidence(result, providedChunks);
                    evaluatedConceptApplicationPort.applyInCurrentTransaction(evaluationId);
                    return true;
                })
                .orElse(false);
    }

    private boolean requireReviewIfOwned(Long evaluationId, String safeReason) {
        return updateIfOwned(evaluationId, evaluation -> evaluation.requireReview(safeReason));
    }

    private boolean failIfOwned(Long evaluationId, String safeReason) {
        return updateIfOwned(evaluationId, evaluation -> evaluation.fail(safeReason));
    }

    private boolean retryOrFail(Long evaluationId, int attemptCount, String safeReason) {
        return updateIfOwned(evaluationId, evaluation -> {
            if (evaluation.hasExhaustedAttempts()) {
                evaluation.fail(safeReason);
            } else {
                evaluation.scheduleRetry(safeReason, LocalDateTime.now(), retryDelay(attemptCount));
            }
        });
    }

    private boolean updateIfOwned(Long evaluationId, Consumer<Evaluation> update) {
        Boolean applied = transactionTemplate.execute(status -> evaluationRepository.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .map(evaluation -> {
                    update.accept(evaluation);
                    return true;
                })
                .orElse(false));
        return Boolean.TRUE.equals(applied);
    }

    private Duration retryDelay(int attemptCount) {
        return retryBaseDelay.multipliedBy(1L << Math.max(0, attemptCount - 1));
    }

}
