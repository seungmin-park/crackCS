package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminEvaluationDetail(
        Long evaluationId,
        Long answerId,
        Long questionId,
        String questionContent,
        String answerContent,
        EvaluationStatus status,
        String failureCode,
        String modelName,
        String evaluatorVersion,
        LocalDateTime occurredAt,
        List<AdminEvaluationEvidence> evidence
) {
    public AdminEvaluationDetail {
        evidence = List.copyOf(evidence);
    }
}
