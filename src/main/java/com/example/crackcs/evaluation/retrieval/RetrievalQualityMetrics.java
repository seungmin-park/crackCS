package com.example.crackcs.evaluation.retrieval;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record RetrievalQualityMetrics(double recallAtK, double irrelevantChunkRate) {

    public static RetrievalQualityMetrics calculate(Set<Long> relevantIds, List<Long> retrievedIds) {
        if (relevantIds == null || relevantIds.isEmpty()) {
            throw new IllegalArgumentException("relevantIds must not be empty");
        }
        if (retrievedIds == null) {
            throw new IllegalArgumentException("retrievedIds must not be null");
        }
        Set<Long> uniqueRetrieved = new HashSet<>(retrievedIds);
        long relevantRetrieved = relevantIds.stream().filter(uniqueRetrieved::contains).count();
        long irrelevant = retrievedIds.stream().filter(id -> !relevantIds.contains(id)).count();
        double recall = (double) relevantRetrieved / relevantIds.size();
        double irrelevantRate = retrievedIds.isEmpty() ? 0.0 : (double) irrelevant / retrievedIds.size();
        return new RetrievalQualityMetrics(recall, irrelevantRate);
    }

    public boolean requiresEmbeddingExperiment() {
        return recallAtK < 0.85 || irrelevantChunkRate > 0.20;
    }
}
