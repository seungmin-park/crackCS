package com.example.crackcs.evaluation.retrieval.benchmark;

import com.example.crackcs.evaluation.retrieval.RetrievalQualityMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RetrievalBenchmarkStatisticsTest {
    @Test
    @DisplayName("검색 누락을 평균 회수율에 포함하고 무관 비율의 질의 평균과 전체 비율을 구분한다")
    void includesEmptyResultsAndSeparatesMacroFromMicro() {
        RetrievalBenchmarkStatistics benchmarkStatistics = RetrievalBenchmarkStatistics.summarize(List.of(
                new RetrievalBenchmarkStatistics.Observation(
                        RetrievalQualityMetrics.calculate(Set.of(10L, 20L), List.of(10L, 30L)), 2),
                new RetrievalBenchmarkStatistics.Observation(
                        RetrievalQualityMetrics.calculate(Set.of(40L), List.of()), 0)));

        assertThat(benchmarkStatistics.queries()).isEqualTo(2);
        assertThat(benchmarkStatistics.macroRecallAtK()).isEqualTo(0.25);
        assertThat(benchmarkStatistics.macroIrrelevantChunkRate()).isEqualTo(0.25);
        assertThat(benchmarkStatistics.microIrrelevantChunkRate()).isEqualTo(0.5);
        assertThat(benchmarkStatistics.emptyResults()).isEqualTo(1);
        assertThat(benchmarkStatistics.requiresEmbeddingExperiment()).isTrue();
    }

    @Test
    @DisplayName("검색 결과가 전부 없어도 회수율 미달을 숨기지 않는다")
    void emptyRetrievalIsNotQualitySuccess() {
        RetrievalBenchmarkStatistics benchmarkStatistics = RetrievalBenchmarkStatistics.summarize(List.of(
                new RetrievalBenchmarkStatistics.Observation(
                        RetrievalQualityMetrics.calculate(Set.of(10L), List.of()), 0)));

        assertThat(benchmarkStatistics.macroRecallAtK()).isZero();
        assertThat(benchmarkStatistics.microIrrelevantChunkRate()).isZero();
        assertThat(benchmarkStatistics.emptyResults()).isEqualTo(1);
        assertThat(benchmarkStatistics.requiresEmbeddingExperiment()).isTrue();
    }

    @Test
    @DisplayName("평가 질의가 없으면 품질 통계 생성을 거부한다")
    void rejectsNoQueries() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RetrievalBenchmarkStatistics.summarize(List.of()));
    }
}
