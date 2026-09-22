package com.example.crackcs.evaluation.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.port.EvaluatedConceptApplicationPort;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
class EvaluationOutcomeCoordinator {

    private final EvaluationRepository evaluationRepository;
    private final TransactionTemplate transactionTemplate;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final EvaluatedConceptApplicationPort evaluatedConceptApplicationPort;
    private final EvaluationCompletionTransaction evaluationCompletionTransaction;
    private final EvaluationOperationLogger evaluationOperationLogger;
    @Value("${crackcs.evaluation.retry-base-delay:1s}")
    private Duration retryBaseDelay;

    void apply(String workerId, ClaimedEvaluationWork claimedWork, EvaluationAttemptOutcome outcome) {
        switch (outcome) {
            case EvaluationAttemptOutcome.Completed completed -> applyCompleted(workerId, claimedWork, completed);
            case EvaluationAttemptOutcome.ReviewRequired review -> applyReview(workerId, claimedWork, review.reason());
            case EvaluationAttemptOutcome.RetryRequired retry -> applyRetry(workerId, claimedWork, retry.reason());
        }
    }

    private void applyReview(String workerId, ClaimedEvaluationWork claimedWork, String reason) {
        if (requireReviewIfOwned(workerId, claimedWork.evaluationId(), reason)) {
            logFailure(claimedWork, reason);
        }
    }

    private void applyRetry(String workerId, ClaimedEvaluationWork claimedWork, String reason) {
        if (retryOrFail(workerId, claimedWork.evaluationId(), claimedWork.attemptCount(), reason)) {
            logFailure(claimedWork, reason);
        }
    }

    private void applyCompleted(
            String workerId,
            ClaimedEvaluationWork claimedWork,
            EvaluationAttemptOutcome.Completed completed
    ) {
        try {
            // A storage conflict retries completion using this same provider result in a fresh transaction.
            AtomicBoolean completionApplied = new AtomicBoolean();
            evaluationCompletionTransaction.execute(() -> completionApplied.set(
                    completeIfOwned(workerId, claimedWork.evaluationId(), completed.evaluationResult(),
                            completed.evidenceChunkIds())));
            if (completionApplied.get()) {
                evaluationOperationLogger.evaluationCompleted(
                        claimedWork.evaluationId(),
                        claimedWork.answerId(),
                        claimedWork.memberId(),
                        completed.evaluationResult().modelName(),
                        completed.evaluationResult().evaluatorVersion()
                );
            }
        } catch (DataIntegrityViolationException permanentStorageFailure) {
            if (failIfOwned(workerId, claimedWork.evaluationId(), "PERSISTENCE_ERROR")) {
                logFailure(claimedWork, "PERSISTENCE_ERROR");
            }
        } catch (ConcurrencyFailureException exhaustedConflict) {
            if (retryOrFail(workerId, claimedWork.evaluationId(), claimedWork.attemptCount(),
                    "PERSISTENCE_CONFLICT")) {
                logFailure(claimedWork, "PERSISTENCE_CONFLICT");
            }
        } catch (IllegalArgumentException invalidResult) {
            if (retryOrFail(workerId, claimedWork.evaluationId(), claimedWork.attemptCount(), "INVALID_RESULT")) {
                logFailure(claimedWork, "INVALID_RESULT");
            }
        }
    }

    private boolean completeIfOwned(
            String workerId,
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

    private boolean requireReviewIfOwned(String workerId, Long evaluationId, String safeReason) {
        return updateIfOwned(workerId, evaluationId, evaluation -> evaluation.requireReview(safeReason));
    }

    private boolean failIfOwned(String workerId, Long evaluationId, String safeReason) {
        return updateIfOwned(workerId, evaluationId, evaluation -> evaluation.fail(safeReason));
    }

    private boolean retryOrFail(String workerId, Long evaluationId, int attemptCount, String safeReason) {
        return updateIfOwned(workerId, evaluationId, evaluation -> {
            if (evaluation.hasExhaustedAttempts()) {
                evaluation.fail(safeReason);
            } else {
                evaluation.scheduleRetry(safeReason, LocalDateTime.now(), retryDelay(attemptCount));
            }
        });
    }

    private boolean updateIfOwned(String workerId, Long evaluationId, Consumer<Evaluation> update) {
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

    private void logFailure(ClaimedEvaluationWork claimedWork, String failureCode) {
        evaluationOperationLogger.evaluationFailed(
                claimedWork.evaluationId(),
                claimedWork.answerId(),
                claimedWork.memberId(),
                failureCode
        );
    }
}
