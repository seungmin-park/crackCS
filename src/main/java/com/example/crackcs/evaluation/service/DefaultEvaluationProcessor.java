package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultEvaluationProcessor implements EvaluationProcessor {
    private static final int MAX_ATTEMPTS = 3;
    private final EvaluationRepository evaluations;
    private final ObjectProvider<EvaluationPort> ports;

    @Override
    @Transactional
    public void process(Long evaluationId) {
        var evaluation = evaluations.findLockedById(evaluationId).orElse(null);
        if (evaluation == null || evaluation.getStatus() != EvaluationStatus.EVALUATING) return;
        var port = ports.getIfAvailable();
        if (port == null) {
            evaluation.fail("PROVIDER_UNAVAILABLE");
            return;
        }
        var answer = evaluation.getAnswer();
        var question = answer.getQuestion();
        var request = new EvaluationRequest(question.getContent(), question.getReferenceAnswer(), answer.getContent(),
                question.getQuestionConcepts().stream().map(qc -> qc.getConcept().getId()).toList());
        String failureReason = "PROVIDER_ERROR";
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                evaluation.complete(port.evaluate(request));
                return;
            } catch (com.example.crackcs.exception.EvaluationTimeoutException timeout) {
                failureReason = "PROVIDER_TIMEOUT";
            } catch (IllegalArgumentException invalidResult) {
                failureReason = "INVALID_RESULT";
            } catch (RuntimeException providerFailure) {
                failureReason = "PROVIDER_ERROR";
            }
        }
        // Never expose provider exception messages (credentials, prompts, raw answers).
        evaluation.fail(failureReason);
    }
}
