package com.example.crackcs.evaluation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class EvaluationCostPolicy {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);

    private final BigDecimal monthlyCapUsd;
    private final BigDecimal inputUsdPerMillionTokens;
    private final BigDecimal outputUsdPerMillionTokens;

    public EvaluationCostPolicy(
            BigDecimal monthlyCapUsd,
            BigDecimal inputUsdPerMillionTokens,
            BigDecimal outputUsdPerMillionTokens
    ) {
        this.monthlyCapUsd = requireNonNegative(monthlyCapUsd, "monthlyCapUsd");
        this.inputUsdPerMillionTokens = requireNonNegative(inputUsdPerMillionTokens, "inputUsdPerMillionTokens");
        this.outputUsdPerMillionTokens = requireNonNegative(outputUsdPerMillionTokens, "outputUsdPerMillionTokens");
    }

    public boolean canEvaluate(long usedInputTokens, long usedOutputTokens) {
        if (usedInputTokens < 0 || usedOutputTokens < 0) {
            throw new IllegalArgumentException("used tokens must not be negative");
        }
        return estimatedUsd(usedInputTokens, usedOutputTokens).compareTo(monthlyCapUsd) < 0;
    }

    public BigDecimal estimatedUsd(long inputTokens, long outputTokens) {
        BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                .multiply(inputUsdPerMillionTokens).divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                .multiply(outputUsdPerMillionTokens).divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        return inputCost.add(outputCost);
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
