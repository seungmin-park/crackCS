package com.example.crackcs.evaluation.controller.response;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.service.AdminEvaluationDetail;

import java.time.LocalDateTime;
import java.util.List;

public record AdminEvaluationDetailResponse(
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
        List<AdminEvaluationEvidenceResponse> evidence
) {
    public static AdminEvaluationDetailResponse from(AdminEvaluationDetail detail) {
        return new AdminEvaluationDetailResponse(
                detail.evaluationId(), detail.answerId(), detail.questionId(), detail.questionContent(),
                detail.answerContent(), detail.status(), detail.failureCode(), detail.modelName(),
                detail.evaluatorVersion(), detail.occurredAt(),
                detail.evidence().stream().map(AdminEvaluationEvidenceResponse::from).toList()
        );
    }
}
