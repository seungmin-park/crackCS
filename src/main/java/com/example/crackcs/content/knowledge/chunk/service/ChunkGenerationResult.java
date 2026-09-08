package com.example.crackcs.content.knowledge.chunk.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;

import java.util.List;

public record ChunkGenerationResult(String generationKey, boolean reused, List<KnowledgeChunk> chunks) {
    public ChunkGenerationResult {
        chunks = List.copyOf(chunks);
    }
}
