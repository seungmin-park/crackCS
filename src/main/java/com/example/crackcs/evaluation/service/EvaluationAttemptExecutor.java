package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.retrieval.KnowledgeRetrievalService;
import com.example.crackcs.evaluation.retrieval.RetrievalResult;
import com.example.crackcs.exception.EvaluationTimeoutException;
import com.example.crackcs.exception.ProviderRequestRejectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class EvaluationAttemptExecutor {

    private static final int EVIDENCE_LIMIT = 5;

    private final ObjectProvider<EvaluationPort> evaluationPortProvider;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final EvaluationBudgetGuard budgetGuard;
    private final EvaluationOperationLogger operationLogger;

    EvaluationAttemptOutcome execute(ClaimedEvaluationWork claimedWork) {
        RetrievalResult retrieval = knowledgeRetrievalService.retrieve(claimedWork.retrievalQuery(), EVIDENCE_LIMIT);
        logRetrieval(claimedWork, retrieval);
        if (retrieval.insufficientEvidence()) {
            return new EvaluationAttemptOutcome.ReviewRequired("EVIDENCE_NOT_FOUND");
        }
        if (retrieval.conflictingEvidence()) {
            return new EvaluationAttemptOutcome.ReviewRequired("EVIDENCE_CONFLICT");
        }
        if (!budgetGuard.canEvaluate()) {
            return new EvaluationAttemptOutcome.ReviewRequired("MONTHLY_BUDGET_EXCEEDED");
        }

        EvaluationPort evaluationPort = availablePort();
        if (evaluationPort == null) {
            return new EvaluationAttemptOutcome.RetryRequired("PROVIDER_UNAVAILABLE");
        }

        EvaluationRequest request = claimedWork.request(retrieval);
        try {
            EvaluationResult result = evaluationPort.evaluate(request);
            List<Long> evidenceChunkIds = retrieval.chunks().stream()
                    .map(retrieved -> retrieved.chunk().getId())
                    .toList();
            return new EvaluationAttemptOutcome.Completed(result, evidenceChunkIds);
        } catch (ProviderRequestRejectedException rejected) {
            return new EvaluationAttemptOutcome.ReviewRequired(rejected.reason());
        } catch (EvaluationTimeoutException timeout) {
            return new EvaluationAttemptOutcome.RetryRequired("PROVIDER_TIMEOUT");
        } catch (IllegalArgumentException invalidResult) {
            return new EvaluationAttemptOutcome.RetryRequired("INVALID_RESULT");
        } catch (RuntimeException providerFailure) {
            return new EvaluationAttemptOutcome.RetryRequired("PROVIDER_ERROR");
        }
    }

    private EvaluationPort availablePort() {
        try {
            return evaluationPortProvider.getIfAvailable();
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private void logRetrieval(ClaimedEvaluationWork claimedWork, RetrievalResult retrieval) {
        List<Long> evidenceChunkIds = retrieval.chunks().stream()
                .map(retrieved -> retrieved.chunk().getId())
                .toList();
        operationLogger.retrievalCompleted(
                claimedWork.evaluationId(),
                claimedWork.answerId(),
                claimedWork.memberId(),
                retrieval.chunks().size(),
                evidenceChunkIds
        );
    }
}
