package com.example.crackcs.evaluation.quality;

import com.example.crackcs.evaluation.domain.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

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
        assertThat(result.binaryAgreement()).isEqualTo(2.0 / 3);
        assertThat(result.falseCorrectRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("검토 필요가 정답인 사례는 삼종 판정과 이진 정확도에서 제외한다")
    void excludesExpectedReviewFromClassification() {
        GoldenSetMetrics.Result result = GoldenSetMetrics.calculate(List.of(
                observation(Verdict.CORRECT, Verdict.CORRECT),
                observation(Verdict.INCORRECT, Verdict.CORRECT),
                observation(Verdict.NEEDS_REVIEW, Verdict.NEEDS_REVIEW)));

        assertThat(result.detailedAgreement()).isEqualTo(0.5);
        assertThat(result.binaryAgreement()).isEqualTo(0.5);
        assertThat(result.falseCorrectRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("채점 가능한 답변에 대한 검토 필요 응답은 정확한 판정으로 계산하지 않는다")
    void countsUnexpectedReviewAsMismatch() {
        GoldenSetMetrics.Result result = GoldenSetMetrics.calculate(List.of(
                observation(Verdict.INCORRECT, Verdict.NEEDS_REVIEW)));

        assertThat(result.detailedAgreement()).isEqualTo(0.0);
        assertThat(result.binaryAgreement()).isEqualTo(0.0);
        assertThat(result.falseCorrectRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("명백한 오답이 없으면 오답 정답 판정 비율을 계산하지 않는다")
    void doesNotReportZeroWithoutIncorrectSamples() {
        GoldenSetMetrics.Result result = GoldenSetMetrics.calculate(List.of(
                observation(Verdict.PARTIALLY_CORRECT, Verdict.CORRECT)));

        assertThat(result.falseCorrectRate()).isNull();
        assertThat(result.binaryAgreement()).isNull();
    }

    @Test
    @DisplayName("검토 필요 사례만 있으면 삼종 판정 일치율을 계산하지 않는다")
    void doesNotReportClassificationForReviewOnly() {
        GoldenSetMetrics.Result result = GoldenSetMetrics.calculate(List.of(
                observation(Verdict.NEEDS_REVIEW, Verdict.NEEDS_REVIEW)));

        assertThat(result.detailedAgreement()).isNull();
    }

    @Test
    @DisplayName("목록 안의 누락된 관측값을 명시적으로 거부한다")
    void rejectsNullEntry() {
        List<GoldenSetMetrics.Observation> observations = new ArrayList<>();
        observations.add(null);

        assertThatThrownBy(() -> GoldenSetMetrics.calculate(observations))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("부분 정답 응답을 이진 판정의 오답 적중으로 처리하지 않는다")
    void doesNotCollapsePartialIntoIncorrect() {
        GoldenSetMetrics.Result result = GoldenSetMetrics.calculate(List.of(
                observation(Verdict.INCORRECT, Verdict.PARTIALLY_CORRECT)));

        assertThat(result.binaryAgreement()).isEqualTo(0.0);
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
