package com.example.crackcs.evaluation.service;

public record AdminEvaluationEvidence(
        Long chunkId,
        String documentTitle,
        int documentVersion,
        int startOffset,
        int endOffset,
        String content
) {
}
