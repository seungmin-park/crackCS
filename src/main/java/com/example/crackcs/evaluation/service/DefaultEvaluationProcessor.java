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
        ClaimedEvaluationWork claimedWork = claimed.orElseThrow();
        applyAttemptOutcome(claimedWork, evaluationAttemptExecutor.execute(claimedWork));
    }

    private void applyAttemptOutcome(ClaimedEvaluationWork claimedWork, EvaluationAttemptOutcome outcome) {
        switch (outcome) {
            case EvaluationAttemptOutcome.Completed completed -> complete(claimedWork, completed);
            case EvaluationAttemptOutcome.ReviewRequired review -> applyReview(claimedWork, review.reason());
            case EvaluationAttemptOutcome.RetryRequired retry -> applyRetry(claimedWork, retry.reason());
        }
    }

    private void applyReview(ClaimedEvaluationWork claimedWork, String reason) {
        if (requireReviewIfOwned(claimedWork.evaluationId(), reason)) {
            logFailure(claimedWork, reason);
        }
    }

    private void applyRetry(ClaimedEvaluationWork claimedWork, String reason) {
        if (retryOrFail(claimedWork.evaluationId(), claimedWork.attemptCount(), reason)) {
            logFailure(claimedWork, reason);
        }
    }

    private void complete(ClaimedEvaluationWork claimedWork, EvaluationAttemptOutcome.Completed completed) {
        try {
            // A storage conflict retries completion using this same provider result in a fresh transaction.
            AtomicBoolean completionApplied = new AtomicBoolean();
            evaluationCompletionTransaction.execute(() -> completionApplied.set(
                    completeIfOwned(claimedWork.evaluationId(), completed.evaluationResult(), completed.evidenceChunkIds())));
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
            if (failIfOwned(claimedWork.evaluationId(), "PERSISTENCE_ERROR")) {
                logFailure(claimedWork, "PERSISTENCE_ERROR");
            }
        } catch (ConcurrencyFailureException exhaustedConflict) {
            if (retryOrFail(claimedWork.evaluationId(), claimedWork.attemptCount(), "PERSISTENCE_CONFLICT")) {
                logFailure(claimedWork, "PERSISTENCE_CONFLICT");
            }
        } catch (IllegalArgumentException invalidResult) {
            if (retryOrFail(claimedWork.evaluationId(), claimedWork.attemptCount(), "INVALID_RESULT")) {
                logFailure(claimedWork, "INVALID_RESULT");
            }
        }
    }

    private void logFailure(ClaimedEvaluationWork claimedWork, String failureCode) {
        evaluationOperationLogger.evaluationFailed(
                claimedWork.evaluationId(),
                claimedWork.answerId(),
                claimedWork.memberId(),
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
