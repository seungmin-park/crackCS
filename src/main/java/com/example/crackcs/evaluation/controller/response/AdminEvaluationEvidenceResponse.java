package com.example.crackcs.evaluation.controller.response;

import com.example.crackcs.evaluation.service.AdminEvaluationEvidence;

public record AdminEvaluationEvidenceResponse(
        Long chunkId,
        String documentTitle,
        int documentVersion,
        int startOffset,
        int endOffset,
        String content
) {
    public static AdminEvaluationEvidenceResponse from(AdminEvaluationEvidence evidence) {
        return new AdminEvaluationEvidenceResponse(
                evidence.chunkId(), evidence.documentTitle(), evidence.documentVersion(),
                evidence.startOffset(), evidence.endOffset(), evidence.content()
        );
    }
}
