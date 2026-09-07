package com.example.crackcs.evaluation.port;

import java.util.List;

public record EvaluationRequest(String questionContent, String referenceAnswer, String answerContent,
                                List<Long> conceptIds) {
    public EvaluationRequest { conceptIds = conceptIds == null ? null : List.copyOf(conceptIds); }
}
