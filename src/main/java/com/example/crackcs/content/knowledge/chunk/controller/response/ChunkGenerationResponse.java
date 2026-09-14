package com.example.crackcs.content.knowledge.chunk.controller.response;

import com.example.crackcs.content.knowledge.chunk.service.ChunkGenerationResult;

import java.util.List;

public record ChunkGenerationResponse(
        String generationKey,
        boolean reused,
        List<KnowledgeChunkResponse> chunks
) {
    public static ChunkGenerationResponse from(ChunkGenerationResult result) {
        return new ChunkGenerationResponse(
                result.generationKey(),
                result.reused(),
                result.chunks().stream().map(KnowledgeChunkResponse::from).toList()
        );
    }
}
