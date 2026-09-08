package com.example.crackcs.evaluation.port;

import java.util.List;

public record EvaluationRequest(
        Long topicId,
        String questionContent,
        String referenceAnswer,
        String answerContent,
        List<EvaluationConceptInput> concepts,
        List<EvaluationEvidenceInput> evidence
) {
    public EvaluationRequest {
        concepts = concepts == null ? null : List.copyOf(concepts);
        evidence = evidence == null ? null : List.copyOf(evidence);
    }

    public EvaluationRequest(
            String questionContent,
            String referenceAnswer,
            String answerContent,
            List<Long> conceptIds
    ) {
        this(null, questionContent, referenceAnswer, answerContent,
                conceptIds == null ? null : conceptIds.stream()
                        .map(id -> new EvaluationConceptInput(id, String.valueOf(id), true)).toList(),
                List.of());
    }

    public List<Long> conceptIds() {
        return concepts == null ? null : concepts.stream().map(EvaluationConceptInput::conceptId).toList();
    }
}
