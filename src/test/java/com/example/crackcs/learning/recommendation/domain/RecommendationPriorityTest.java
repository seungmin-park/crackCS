package com.example.crackcs.learning.recommendation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationPriorityTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 13, 12, 0);

    @Test
    @DisplayName("미평가를 먼저 선택하고 평가된 개념끼리는 낮은 숙련도를 우선한다")
    void prioritizesUnassessedThenLowMastery() {
        assertThat(new RecommendationPriority(null, now, 9L, 9L))
                .isLessThan(new RecommendationPriority(0.0, null, 1L, 1L));
        assertThat(new RecommendationPriority(0.0, now, 9L, 9L))
                .isLessThan(new RecommendationPriority(50.0, null, 1L, 1L));
    }

    @Test
    @DisplayName("숙련도가 같으면 미풀이 다음 마지막 풀이가 오래된 문제를 우선한다")
    void prioritizesUnansweredThenOlderAnswer() {
        assertThat(new RecommendationPriority(50.0, null, 9L, 9L))
                .isLessThan(new RecommendationPriority(50.0, now, 1L, 1L));
        assertThat(new RecommendationPriority(50.0, now.minusDays(1), 9L, 9L))
                .isLessThan(new RecommendationPriority(50.0, now, 1L, 1L));
    }

    @Test
    @DisplayName("나머지 조건이 같으면 문제와 개념 식별자로 추천을 결정한다")
    void resolvesFinalTiesDeterministically() {
        assertThat(new RecommendationPriority(null, null, 1L, 9L))
                .isLessThan(new RecommendationPriority(null, null, 2L, 1L));
        assertThat(new RecommendationPriority(null, null, 1L, 1L))
                .isLessThan(new RecommendationPriority(null, null, 1L, 2L));
    }
}
