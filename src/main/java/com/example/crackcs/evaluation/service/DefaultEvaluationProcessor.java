package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DefaultEvaluationProcessor implements EvaluationProcessor {
    private static final int MAX_ATTEMPTS = 3;
    private final EvaluationRepository evaluations;
    private final ObjectProvider<EvaluationPort> ports;
    private final TransactionTemplate transactions;
    private final Object[] localLocks = IntStream.range(0, 64).mapToObj(ignored -> new Object()).toArray();

    @Override
    public void process(Long evaluationId) {
        Object localLock = localLocks[Math.floorMod(evaluationId.hashCode(), localLocks.length)];
        synchronized (localLock) {
            processSerially(evaluationId);
        }
    }

    private void processSerially(Long evaluationId) {
        EvaluationRequest request = transactions.execute(status -> prepareRequest(evaluationId));
        if (request == null) return;
        EvaluationPort port;
        try {
            port = ports.getIfAvailable();
        } catch (RuntimeException unavailable) {
            failIfPending(evaluationId, "PROVIDER_UNAVAILABLE");
            return;
        }
        if (port == null) {
            failIfPending(evaluationId, "PROVIDER_UNAVAILABLE");
            return;
        }
        String failureReason = "PROVIDER_ERROR";
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                var result = port.evaluate(request);
                transactions.executeWithoutResult(status -> completeIfPending(evaluationId, result));
                return;
            } catch (com.example.crackcs.exception.EvaluationTimeoutException timeout) {
                failureReason = "PROVIDER_TIMEOUT";
            } catch (IllegalArgumentException invalidResult) {
                failureReason = "INVALID_RESULT";
            } catch (RuntimeException providerFailure) {
                failureReason = "PROVIDER_ERROR";
            }
        }
        failIfPending(evaluationId, failureReason);
    }

    private EvaluationRequest prepareRequest(Long evaluationId) {
        var evaluation = evaluations.findLockedById(evaluationId).orElse(null);
        if (evaluation == null || evaluation.getStatus() != EvaluationStatus.EVALUATING) return null;
        var answer = evaluation.getAnswer();
        var question = answer.getQuestion();
        return new EvaluationRequest(question.getContent(), question.getReferenceAnswer(), answer.getContent(),
                question.getQuestionConcepts().stream().map(qc -> qc.getConcept().getId()).toList());
    }

    private void completeIfPending(Long evaluationId, com.example.crackcs.evaluation.port.EvaluationResult result) {
        evaluations.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.getStatus() == EvaluationStatus.EVALUATING)
                .ifPresent(evaluation -> evaluation.complete(result));
    }

    private void failIfPending(Long evaluationId, String safeReason) {
        transactions.executeWithoutResult(status -> evaluations.findLockedById(evaluationId)
                .filter(evaluation -> evaluation.getStatus() == EvaluationStatus.EVALUATING)
                .ifPresent(evaluation -> evaluation.fail(safeReason)));
    }
}
