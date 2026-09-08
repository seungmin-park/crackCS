package com.example.crackcs.evaluation.retrieval;

import java.util.List;

public record RetrievalResult(
        List<RetrievedChunk> chunks,
        boolean insufficientEvidence,
        boolean conflictingEvidence
) {
    public RetrievalResult {
        chunks = List.copyOf(chunks);
    }
}
