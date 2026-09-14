package com.example.crackcs.evaluation.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.port.*;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.retrieval.KnowledgeRetrievalService;
import com.example.crackcs.evaluation.retrieval.RetrievalQuery;
import com.example.crackcs.evaluation.retrieval.RetrievalResult;
import com.example.crackcs.exception.EvaluationTimeoutException;
import com.example.crackcs.learning.answer.domain.Answer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DefaultEvaluationProcessor implements EvaluationProcessor {
    private final EvaluationRepository evaluationRepository;
    private final ObjectProvider<EvaluationPort> ports;
    private final TransactionTemplate transactionTemplate;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final EvaluationBudgetGuard budgetGuard;
    private final EvaluatedConceptApplicationPort evaluatedConcepts;
    private final EvaluationCompletionTransaction completionTransactions;
    private final String workerId = UUID.randomUUID().toString();
    private final Object[] localLocks = IntStream.range(0, 64).mapToObj(ignored -> new Object()).toArray();
    @Value("${crackcs.evaluation.lease-duration:1m}")
    private Duration leaseDuration;
    @Value("${crackcs.evaluation.retry-base-delay:1s}")
    private Duration retryBaseDelay;

    @Override
    public void process(Long evaluationId) {
        Object localLock = localLocks[Math.floorMod(evaluationId.hashCode(), localLocks.length)];
        synchronized (localLock) {
            processSerially(evaluationId);
        }
    }

    private void processSerially(Long evaluationId) {
        Optional<PendingEvaluation> claimed = Objects.requireNonNull(
                transactionTemplate.execute(status -> claim(evaluationId)),
                "claim transaction must return an Optional");
        if (claimed.isEmpty()) {
            return;
        }
        PendingEvaluation pending = claimed.orElseThrow();
        RetrievalResult retrieval = knowledgeRetrievalService.retrieve(pending.retrievalQuery(), 5);
        if (retrieval.insufficientEvidence()) {
            requireReviewIfOwned(evaluationId, "EVIDENCE_NOT_FOUND");
            return;
        }
        if (retrieval.conflictingEvidence()) {
            requireReviewIfOwned(evaluationId, "EVIDENCE_CONFLICT");
            return;
        }
        EvaluationRequest request = pending.request(retrieval);
        if (!budgetGuard.canEvaluate()) {
            requireReviewIfOwned(evaluationId, "MONTHLY_BUDGET_EXCEEDED");
            return;
        }
        EvaluationPort port;
        try {
            port = ports.getIfAvailable();
        } catch (RuntimeException unavailable) {
            retryOrFail(evaluationId, pending.attemptCount(), "PROVIDER_UNAVAILABLE");
            return;
        }
        if (port == null) {
            retryOrFail(evaluationId, pending.attemptCount(), "PROVIDER_UNAVAILABLE");
            return;
        }
        EvaluationResult result;
        try {
            result = port.evaluate(request);
        } catch (EvaluationTimeoutException timeout) {
            retryOrFail(evaluationId, pending.attemptCount(), "PROVIDER_TIMEOUT");
            return;
        } catch (IllegalArgumentException invalidResult) {
            retryOrFail(evaluationId, pending.attemptCount(), "INVALID_RESULT");
            return;
        } catch (RuntimeException providerFailure) {
            retryOrFail(evaluationId, pending.attemptCount(), "PROVIDER_ERROR");
            return;
        }
        List<Long> providedChunkIds = retrieval.chunks().stream()
                .map(retrieved -> retrieved.chunk().getId()).toList();
        try {
            // A storage conflict retries completion using this same provider result in a fresh transaction.
            completionTransactions.execute(() -> completeIfOwned(evaluationId, result, providedChunkIds));
        } catch (DataIntegrityViolationException permanentStorageFailure) {
            failIfOwned(evaluationId, "PERSISTENCE_ERROR");
        } catch (ConcurrencyFailureException exhaustedConflict) {
            retryOrFail(evaluationId, pending.attemptCount(), "PERSISTENCE_CONFLICT");
        } catch (IllegalArgumentException invalidResult) {
            retryOrFail(evaluationId, pending.attemptCount(), "INVALID_RESULT");
        }
    }

    private Optional<PendingEvaluation> claim(Long evaluationId) {
        Optional<Evaluation> found = evaluationRepository.findLockedById(evaluationId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Evaluation evaluation = found.orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        if (!evaluation.claim(workerId, now, leaseDuration)) {
            return Optional.empty();
        }
        return Optional.of(toPendingEvaluation(evaluation));
    }

    private PendingEvaluation toPendingEvaluation(Evaluation evaluation) {
        Answer answer = evaluation.getAnswer();
        Question question = answer.getQuestion();
        List<EvaluationConceptInput> concepts = question.getQuestionConcepts().stream()
                .map(qc -> new EvaluationConceptInput(qc.getConcept().getId(), qc.getConcept().getName(),
                        qc.isRequired()))
                .toList();
        return new PendingEvaluation(
                question.getTopic().getId(), question.getContent(), question.getReferenceAnswer(),
                answer.getContent(), concepts, evaluation.getAttemptCount()
        );
    }

    private void completeIfOwned(
            Long evaluationId,
            EvaluationResult result,
            List<Long> providedChunkIds
    ) {
        List<KnowledgeChunk> providedChunks = knowledgeChunkRepository.findAllById(providedChunkIds);
        evaluationRepository.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .ifPresent(evaluation -> {
                    evaluation.completeWithEvidence(result, providedChunks);
                    evaluatedConcepts.applyInCurrentTransaction(evaluationId);
                });
    }

    private void requireReviewIfOwned(Long evaluationId, String safeReason) {
        transactionTemplate.executeWithoutResult(status -> evaluationRepository.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .ifPresent(evaluation -> evaluation.requireReview(safeReason)));
    }

    private void failIfOwned(Long evaluationId, String safeReason) {
        transactionTemplate.executeWithoutResult(status -> evaluationRepository.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .ifPresent(evaluation -> evaluation.fail(safeReason)));
    }

    private void retryOrFail(Long evaluationId, int attemptCount, String safeReason) {
        transactionTemplate.executeWithoutResult(status -> evaluationRepository.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .ifPresent(evaluation -> {
                    if (evaluation.hasExhaustedAttempts()) {
                        evaluation.fail(safeReason);
                    } else {
                        evaluation.scheduleRetry(safeReason, LocalDateTime.now(), retryDelay(attemptCount));
                    }
                }));
    }

    private Duration retryDelay(int attemptCount) {
        return retryBaseDelay.multipliedBy(1L << Math.max(0, attemptCount - 1));
    }

    private record PendingEvaluation(
            Long topicId,
            String question,
            String referenceAnswer,
            String answer,
            List<EvaluationConceptInput> concepts,
            int attemptCount
    ) {
        RetrievalQuery retrievalQuery() {
            return new RetrievalQuery(
                    topicId,
                    concepts.stream().map(EvaluationConceptInput::name).toList(),
                    question,
                    referenceAnswer,
                    answer
            );
        }

        EvaluationRequest request(RetrievalResult retrieval) {
            List<EvaluationEvidenceInput> evidence = retrieval.chunks().stream()
                    .map(retrieved -> {
                        KnowledgeChunk chunk = retrieved.chunk();
                        KnowledgeDocument document = chunk.getDocument();
                        return new EvaluationEvidenceInput(
                                chunk.getId(),
                                document.getId(),
                                document.getTitle(),
                                document.getDocumentVersion(),
                                chunk.getStartOffset(),
                                chunk.getEndOffset(),
                                chunk.getContent(),
                                retrieved.relevanceScore()
                        );
                    })
                    .toList();
            return new EvaluationRequest(topicId, question, referenceAnswer, answer, concepts, evidence);
        }
    }
}
