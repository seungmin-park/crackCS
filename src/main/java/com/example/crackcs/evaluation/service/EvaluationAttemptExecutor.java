package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.retrieval.KnowledgeRetrievalService;
import com.example.crackcs.evaluation.retrieval.RetrievalResult;
import com.example.crackcs.exception.EvaluationTimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class EvaluationAttemptExecutor {

    private static final int EVIDENCE_LIMIT = 5;

    private final ObjectProvider<EvaluationPort> evaluationPorts;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final EvaluationBudgetGuard budgetGuard;
    private final EvaluationOperationLogger operationLogger;

    EvaluationAttempt execute(ClaimedEvaluationWork work) {
        RetrievalResult retrieval = knowledgeRetrievalService.retrieve(work.retrievalQuery(), EVIDENCE_LIMIT);
        logRetrieval(work, retrieval);
        if (retrieval.insufficientEvidence()) {
            return new EvaluationAttempt.ReviewRequired("EVIDENCE_NOT_FOUND");
        }
        if (retrieval.conflictingEvidence()) {
            return new EvaluationAttempt.ReviewRequired("EVIDENCE_CONFLICT");
        }
        if (!budgetGuard.canEvaluate()) {
            return new EvaluationAttempt.ReviewRequired("MONTHLY_BUDGET_EXCEEDED");
        }

        EvaluationPort evaluationPort = availablePort();
        if (evaluationPort == null) {
            return new EvaluationAttempt.RetryRequired("PROVIDER_UNAVAILABLE");
        }

        EvaluationRequest request = work.request(retrieval);
        try {
            EvaluationResult result = evaluationPort.evaluate(request);
            List<Long> evidenceChunkIds = retrieval.chunks().stream()
                    .map(retrieved -> retrieved.chunk().getId())
                    .toList();
            return new EvaluationAttempt.Completed(result, evidenceChunkIds);
        } catch (EvaluationTimeoutException timeout) {
            return new EvaluationAttempt.RetryRequired("PROVIDER_TIMEOUT");
        } catch (IllegalArgumentException invalidResult) {
            return new EvaluationAttempt.RetryRequired("INVALID_RESULT");
        } catch (RuntimeException providerFailure) {
            return new EvaluationAttempt.RetryRequired("PROVIDER_ERROR");
        }
    }

    private EvaluationPort availablePort() {
        try {
            return evaluationPorts.getIfAvailable();
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private void logRetrieval(ClaimedEvaluationWork work, RetrievalResult retrieval) {
        List<Long> evidenceChunkIds = retrieval.chunks().stream()
                .map(retrieved -> retrieved.chunk().getId())
                .toList();
        operationLogger.retrievalCompleted(
                work.evaluationId(),
                work.answerId(),
                work.memberId(),
                retrieval.chunks().size(),
                evidenceChunkIds
        );
    }
}
