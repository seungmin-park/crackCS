package com.example.crackcs.evaluation.retrieval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalQualityMetricsTest {

    @Test
    @DisplayName("정답 Chunk 회수율과 관련 없는 Chunk 포함률을 계산한다")
    void calculatesRecallAndIrrelevantRate() {
        RetrievalQualityMetrics metrics = RetrievalQualityMetrics.calculate(
                Set.of(1L, 2L), List.of(1L, 3L, 2L, 4L)
        );

        assertThat(metrics.recallAtK()).isEqualTo(1.0);
        assertThat(metrics.irrelevantChunkRate()).isEqualTo(0.5);
        assertThat(metrics.requiresEmbeddingExperiment()).isTrue();
    }
}
