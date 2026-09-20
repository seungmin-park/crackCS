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
        List<Observation> classified = observations.stream()
                .filter(value -> value.expected != Verdict.NEEDS_REVIEW).toList();
        List<Observation> binary = classified.stream()
                .filter(value -> value.expected != Verdict.PARTIALLY_CORRECT).toList();
        List<Observation> incorrect = classified.stream()
                .filter(value -> value.expected == Verdict.INCORRECT).toList();
        long detailedMatches = classified.stream()
                .filter(value -> value.expected == value.actual)
                .count();
        long binaryMatches = binary.stream()
                .filter(value -> value.expected == value.actual)
                .count();
        long falseCorrect = incorrect.stream()
                .filter(value -> value.actual == Verdict.CORRECT)
                .count();

        return new Result(ratio(detailedMatches, classified.size()),
                ratio(binaryMatches, binary.size()), ratio(falseCorrect, incorrect.size()));
    }

    private static boolean hasInsufficientDataForMetrics(List<Observation> observations) {
        return observations == null
                || observations.isEmpty()
                || observations.stream().anyMatch(value -> value == null || value.isIncomplete());
    }

    private static Double ratio(long matches, long total) {
        return total == 0 ? null : (double) matches / total;
    }

    public record Observation(Verdict expected, Verdict actual) {
        private boolean isIncomplete() {
            return expected == null || actual == null;
        }
    }

    public record Result(Double detailedAgreement, Double binaryAgreement, Double falseCorrectRate) {
    }
}
