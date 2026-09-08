package com.example.crackcs.evaluation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationCostPolicyTest {

    private final EvaluationCostPolicy policy = new EvaluationCostPolicy(
            new BigDecimal("50.00"), new BigDecimal("2.00"), new BigDecimal("12.00")
    );

    @Test
    @DisplayName("월 누적 추정 비용이 상한보다 낮으면 다음 평가를 허용한다")
    void allowsEvaluationBelowMonthlyCap() {
        assertThat(policy.canEvaluate(10_000_000L, 1_000_000L)).isTrue();
    }

    @Test
    @DisplayName("월 누적 추정 비용이 상한에 도달하면 다음 평가를 차단한다")
    void blocksEvaluationAtMonthlyCap() {
        assertThat(policy.canEvaluate(21_000_000L, 1_000_000L)).isFalse();
    }
}
