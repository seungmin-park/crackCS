package com.example.crackcs.content.knowledge.chunk.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;

import java.util.List;

public interface KnowledgeChunkService {

    ChunkGenerationResult generateChunks(Long documentId);

    List<KnowledgeChunk> findByDocumentId(Long documentId);
}
