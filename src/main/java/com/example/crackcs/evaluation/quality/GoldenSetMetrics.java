package com.example.crackcs.evaluation.quality;

import com.example.crackcs.evaluation.domain.Verdict;

import java.util.List;

public final class GoldenSetMetrics {
    private GoldenSetMetrics() {
    }

    public static Result calculate(List<Observation> observations) {
        if (hasInsufficientDataForMetrics(observations)) {
            throw new IllegalArgumentException("observations must contain expected and actual verdicts");
        }
        long detailedMatches = observations.stream()
                .filter(value -> value.expected == value.actual)
                .count();
        long binaryMatches = observations.stream()
                .filter(value -> correct(value.expected) == correct(value.actual))
                .count();
        long falseCorrect = observations.stream()
                .filter(value -> !correct(value.expected) && correct(value.actual))
                .count();

        double size = observations.size();
        return new Result(detailedMatches / size, binaryMatches / size, falseCorrect / size);
    }

    private static boolean hasInsufficientDataForMetrics(List<Observation> observations) {
        return observations == null
                || observations.isEmpty()
                || observations.stream().anyMatch(Observation::isIncomplete);
    }

    private static boolean correct(Verdict verdict) {
        return verdict == Verdict.CORRECT;
    }

    public record Observation(Verdict expected, Verdict actual) {
        private boolean isIncomplete() {
            return expected == null || actual == null;
        }
    }

    public record Result(double detailedAgreement, double binaryAgreement, double falseCorrectRate) {
    }
}
