package com.example.crackcs.evaluation.retrieval;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;

public record RetrievedChunk(KnowledgeChunk chunk, double relevanceScore) {
}
