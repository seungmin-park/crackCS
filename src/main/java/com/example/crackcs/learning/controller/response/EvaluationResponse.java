package com.example.crackcs.learning.controller.response;

import com.example.crackcs.evaluation.domain.*;
import java.util.List;

public record EvaluationResponse(EvaluationStatus status, Verdict verdict, Integer score,
        String feedback, String failureReason, List<ConceptResponse> concepts) {
    public static EvaluationResponse from(Evaluation evaluation) {
        return new EvaluationResponse(evaluation.getStatus(), evaluation.getVerdict(), evaluation.getScore(),
                evaluation.getFeedback(), evaluation.getFailureReason(), evaluation.getConcepts().stream()
                .map(c -> new ConceptResponse(c.getConceptId(), c.getVerdict(), c.getScore(), c.getFeedback())).toList());
    }
    public record ConceptResponse(Long conceptId, Verdict verdict, Integer score, String feedback) {}
}
