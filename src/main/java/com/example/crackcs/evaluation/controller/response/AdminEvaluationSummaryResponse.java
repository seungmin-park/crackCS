package com.example.crackcs.evaluation.controller.response;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.service.AdminEvaluationSummary;

import java.time.LocalDateTime;

public record AdminEvaluationSummaryResponse(
        Long evaluationId,
        Long answerId,
        Long questionId,
        EvaluationStatus status,
        String failureCode,
        String modelName,
        String evaluatorVersion,
        LocalDateTime occurredAt
) {
    public static AdminEvaluationSummaryResponse from(AdminEvaluationSummary summary) {
        return new AdminEvaluationSummaryResponse(
                summary.evaluationId(), summary.answerId(), summary.questionId(), summary.status(),
                summary.failureCode(), summary.modelName(), summary.evaluatorVersion(), summary.occurredAt()
        );
    }
}
