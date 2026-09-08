package com.example.crackcs.evaluation.port;

public record EvaluationEvidenceInput(
        Long chunkId,
        Long documentId,
        String documentTitle,
        int documentVersion,
        int startOffset,
        int endOffset,
        String content,
        double relevanceScore
) {
}
