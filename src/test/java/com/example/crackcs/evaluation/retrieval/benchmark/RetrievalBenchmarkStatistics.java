package com.example.crackcs.evaluation.retrieval.benchmark;

import com.example.crackcs.evaluation.retrieval.RetrievalQualityMetrics;

import java.util.List;

record RetrievalBenchmarkStatistics(int queries, double macroRecallAtK, double macroIrrelevantChunkRate,
                                    double microIrrelevantChunkRate, long emptyResults,
                                    boolean requiresEmbeddingExperiment) {
    record Observation(RetrievalQualityMetrics metrics, int retrievedCount) {}

    static RetrievalBenchmarkStatistics summarize(List<Observation> observations) {
        if (observations.isEmpty()) {
            throw new IllegalArgumentException("At least one query is required");
        }
        double recall = observations.stream().mapToDouble(row -> row.metrics().recallAtK()).average().orElseThrow();
        double macroIrrelevant = observations.stream()
                .mapToDouble(row -> row.metrics().irrelevantChunkRate()).average().orElseThrow();
        long retrieved = observations.stream().mapToLong(Observation::retrievedCount).sum();
        double irrelevant = observations.stream()
                .mapToDouble(row -> row.metrics().irrelevantChunkRate() * row.retrievedCount()).sum();
        double microIrrelevant = retrieved == 0 ? 0 : irrelevant / retrieved;
        long empty = observations.stream().filter(row -> row.retrievedCount() == 0).count();
        boolean experiment = new RetrievalQualityMetrics(recall, macroIrrelevant).requiresEmbeddingExperiment();
        return new RetrievalBenchmarkStatistics(observations.size(), recall, macroIrrelevant,
                microIrrelevant, empty, experiment);
    }
}
