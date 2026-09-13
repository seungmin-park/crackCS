package com.example.crackcs.learning.recommendation.domain;

import java.time.LocalDateTime;
import java.util.Comparator;

public record RecommendationPriority(Double masteryScore, LocalDateTime lastAnsweredAt, Long questionId, Long conceptId)
        implements Comparable<RecommendationPriority> {
    private static final Comparator<RecommendationPriority> ORDER = Comparator
            .comparing(RecommendationPriority::masteryScore, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(RecommendationPriority::lastAnsweredAt, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(RecommendationPriority::questionId)
            .thenComparing(RecommendationPriority::conceptId);

    @Override
    public int compareTo(RecommendationPriority other) {
        return ORDER.compare(this, other);
    }
}
