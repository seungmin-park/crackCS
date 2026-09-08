package com.example.crackcs.evaluation.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.port.EvaluationConceptInput;
import com.example.crackcs.evaluation.port.EvaluationEvidenceInput;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.port.EvaluationResult;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.retrieval.KnowledgeRetrievalService;
import com.example.crackcs.evaluation.retrieval.RetrievalQuery;
import com.example.crackcs.evaluation.retrieval.RetrievalResult;
import com.example.crackcs.exception.EvaluationTimeoutException;
import com.example.crackcs.learning.domain.Answer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.IntStream;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultEvaluationProcessor implements EvaluationProcessor {
    private static final int MAX_ATTEMPTS = 3;
    private final EvaluationRepository evaluations;
    private final ObjectProvider<EvaluationPort> ports;
    private final TransactionTemplate transactions;
    private final KnowledgeRetrievalService retrievalService;
    private final KnowledgeChunkRepository chunks;
    private final EvaluationBudgetGuard budgetGuard;
    private final String workerId = UUID.randomUUID().toString();
    @Value("${crackcs.evaluation.lease-duration:1m}")
    private Duration leaseDuration;
    @Value("${crackcs.evaluation.retry-base-delay:1s}")
    private Duration retryBaseDelay;
    private final Object[] localLocks = IntStream.range(0, 64).mapToObj(ignored -> new Object()).toArray();

    @Override
    public void process(Long evaluationId) {
        Object localLock = localLocks[Math.floorMod(evaluationId.hashCode(), localLocks.length)];
        synchronized (localLock) {
            processSerially(evaluationId);
        }
    }

    private void processSerially(Long evaluationId) {
        PendingEvaluation pending = transactions.execute(status -> claim(evaluationId));
        if (pending == null) {
            return;
        }
        RetrievalResult retrieval = retrievalService.retrieve(pending.retrievalQuery(), 5);
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
        try {
            EvaluationResult result = port.evaluate(request);
            List<Long> providedChunkIds = retrieval.chunks().stream()
                    .map(retrieved -> retrieved.chunk().getId()).toList();
            transactions.executeWithoutResult(status -> completeIfOwned(evaluationId, result, providedChunkIds));
        } catch (EvaluationTimeoutException timeout) {
            retryOrFail(evaluationId, pending.attemptCount(), "PROVIDER_TIMEOUT");
        } catch (IllegalArgumentException invalidResult) {
            retryOrFail(evaluationId, pending.attemptCount(), "INVALID_RESULT");
        } catch (RuntimeException providerFailure) {
            retryOrFail(evaluationId, pending.attemptCount(), "PROVIDER_ERROR");
        }
    }

    private PendingEvaluation claim(Long evaluationId) {
        Evaluation evaluation = evaluations.findLockedById(evaluationId)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (evaluation == null || !evaluation.claim(workerId, now, leaseDuration)) {
            return null;
        }
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
        List<KnowledgeChunk> providedChunks = chunks.findAllById(providedChunkIds);
        evaluations.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .ifPresent(evaluation -> evaluation.completeWithEvidence(result, providedChunks));
    }

    private void requireReviewIfOwned(Long evaluationId, String safeReason) {
        transactions.executeWithoutResult(status -> evaluations.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .ifPresent(evaluation -> evaluation.requireReview(safeReason)));
    }

    private void retryOrFail(Long evaluationId, int attemptCount, String safeReason) {
        transactions.executeWithoutResult(status -> evaluations.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.hasActiveLease(workerId, LocalDateTime.now()))
                .ifPresent(evaluation -> {
                    if (attemptCount >= MAX_ATTEMPTS) {
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
