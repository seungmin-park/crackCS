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
        RetrievalBenchmarkStatistics result = RetrievalBenchmarkStatistics.summarize(List.of(
                new RetrievalBenchmarkStatistics.Observation(
                        RetrievalQualityMetrics.calculate(Set.of(10L, 20L), List.of(10L, 30L)), 2),
                new RetrievalBenchmarkStatistics.Observation(
                        RetrievalQualityMetrics.calculate(Set.of(40L), List.of()), 0)));

        assertThat(result.queries()).isEqualTo(2);
        assertThat(result.macroRecallAtK()).isEqualTo(0.25);
        assertThat(result.macroIrrelevantChunkRate()).isEqualTo(0.25);
        assertThat(result.microIrrelevantChunkRate()).isEqualTo(0.5);
        assertThat(result.emptyResults()).isEqualTo(1);
        assertThat(result.requiresEmbeddingExperiment()).isTrue();
    }

    @Test
    @DisplayName("검색 결과가 전부 없어도 회수율 미달을 숨기지 않는다")
    void emptyRetrievalIsNotQualitySuccess() {
        RetrievalBenchmarkStatistics result = RetrievalBenchmarkStatistics.summarize(List.of(
                new RetrievalBenchmarkStatistics.Observation(
                        RetrievalQualityMetrics.calculate(Set.of(10L), List.of()), 0)));

        assertThat(result.macroRecallAtK()).isZero();
        assertThat(result.microIrrelevantChunkRate()).isZero();
        assertThat(result.emptyResults()).isEqualTo(1);
        assertThat(result.requiresEmbeddingExperiment()).isTrue();
    }

    @Test
    @DisplayName("평가 질의가 없으면 품질 통계 생성을 거부한다")
    void rejectsNoQueries() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RetrievalBenchmarkStatistics.summarize(List.of()));
    }
}
