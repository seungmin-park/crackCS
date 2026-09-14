package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationStatus;

import java.time.LocalDateTime;

public record AdminEvaluationSummary(
        Long evaluationId,
        Long answerId,
        Long questionId,
        EvaluationStatus status,
        String failureCode,
        String modelName,
        String evaluatorVersion,
        LocalDateTime occurredAt
) {
}
