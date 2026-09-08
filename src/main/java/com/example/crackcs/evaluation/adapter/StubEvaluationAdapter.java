package com.example.crackcs.evaluation.adapter;

import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.port.ConceptResult;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.port.EvaluationResult;
import com.example.crackcs.exception.EvaluationTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("(local | test) & !prod & !production")
@ConditionalOnProperty(name = "crackcs.evaluation.openai.enabled", havingValue = "false", matchIfMissing = true)
public class StubEvaluationAdapter implements EvaluationPort {
    private final StubOutcome outcome;

    public StubEvaluationAdapter(@Value("${crackcs.evaluation.stub-outcome:CORRECT}") String outcome) {
        try {
            this.outcome = StubOutcome.valueOf(outcome.toUpperCase());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("unsupported stub outcome: " + outcome, exception);
        }
    }

    @Override
    public EvaluationResult evaluate(EvaluationRequest request) {
        if (request == null || request.conceptIds() == null) {
            throw new IllegalArgumentException("request and conceptIds are required");
        }
        if (outcome == StubOutcome.TIMEOUT) {
            throw new EvaluationTimeoutException();
        }
        if (outcome == StubOutcome.FAILURE) {
            throw new IllegalStateException("evaluation stub failure");
        }
        Verdict verdict = Verdict.valueOf(outcome.name());
        List<ConceptResult> concepts = request.conceptIds().stream()
                .map(id -> new ConceptResult(id, verdict, "stub " + verdict.name().toLowerCase()))
                .toList();
        return new EvaluationResult(
                verdict,
                "stub " + verdict.name().toLowerCase(),
                concepts,
                List.of("stub strength"),
                List.of(),
                List.of(),
                request.evidence().stream().map(evidence -> evidence.chunkId()).toList(),
                "stub",
                "phase5-stub-v1",
                0,
                0,
                0
        );
    }

    private enum StubOutcome {CORRECT, PARTIALLY_CORRECT, INCORRECT, NEEDS_REVIEW, TIMEOUT, FAILURE}
}
