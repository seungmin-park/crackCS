package com.example.crackcs.evaluation.quality;

import com.example.crackcs.evaluation.domain.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoldenSetMetricsTest {

    @Test
    @DisplayName("상세 판정 일치율과 이진 일치율 및 false-correct 비율을 계산한다")
    void calculatesLaunchMetrics() {
        List<GoldenSetMetrics.Observation> observations = List.of(
                observation(Verdict.CORRECT, Verdict.CORRECT),
                observation(Verdict.PARTIALLY_CORRECT, Verdict.INCORRECT),
                observation(Verdict.INCORRECT, Verdict.INCORRECT),
                observation(Verdict.INCORRECT, Verdict.CORRECT)
        );

        GoldenSetMetrics.Result result = GoldenSetMetrics.calculate(observations);

        assertThat(result.detailedAgreement()).isEqualTo(0.5);
        assertThat(result.binaryAgreement()).isEqualTo(0.75);
        assertThat(result.falseCorrectRate()).isEqualTo(0.25);
    }

    @Test
    @DisplayName("관찰 결과 목록이 없으면 골든 세트 지표를 계산할 수 없다")
    void rejectsNullObservations() {
        assertThatThrownBy(() -> GoldenSetMetrics.calculate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("observations must contain expected and actual verdicts");
    }

    @Test
    @DisplayName("관찰 결과가 없으면 골든 세트 지표를 계산할 수 없다")
    void rejectsEmptyObservations() {
        assertThatThrownBy(() -> GoldenSetMetrics.calculate(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("observations must contain expected and actual verdicts");
    }

    @Test
    @DisplayName("기대 판정과 실제 판정 중 하나라도 없으면 골든 세트 지표를 계산할 수 없다")
    void rejectsObservationWithoutBothVerdicts() {
        List<GoldenSetMetrics.Observation> observations = List.of(
                observation(Verdict.CORRECT, null)
        );

        assertThatThrownBy(() -> GoldenSetMetrics.calculate(observations))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("observations must contain expected and actual verdicts");
    }

    private GoldenSetMetrics.Observation observation(Verdict expected, Verdict actual) {
        return new GoldenSetMetrics.Observation(expected, actual);
    }
}
