package com.example.crackcs.content.knowledge.chunk.controller.response;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunkSearchStatus;

public record KnowledgeChunkResponse(
        Long id,
        int sequenceNo,
        int startOffset,
        int endOffset,
        String content,
        String checksum,
        String generationKey,
        KnowledgeChunkSearchStatus searchStatus
) {
    public static KnowledgeChunkResponse from(KnowledgeChunk chunk) {
        return new KnowledgeChunkResponse(
                chunk.getId(),
                chunk.getSequenceNo(),
                chunk.getStartOffset(),
                chunk.getEndOffset(),
                chunk.getContent(),
                chunk.getChecksum(),
                chunk.getGenerationKey(),
                chunk.getSearchStatus()
        );
    }
}
