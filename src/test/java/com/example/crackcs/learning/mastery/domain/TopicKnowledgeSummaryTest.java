package com.example.crackcs.learning.mastery.domain;

import com.example.crackcs.learning.mastery.domain.TopicKnowledgeSummary.ConceptKnowledge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TopicKnowledgeSummaryTest {
    @Test
    @DisplayName("미평가 개념과 오답으로 0점인 개념을 구분한다")
    void distinguishesUnknownFromZeroMastery() {
        ConceptKnowledge unknown = new ConceptKnowledge(1L, "미평가", KnowledgeStatus.UNKNOWN, null, 0, 0, null);
        ConceptKnowledge incorrect = new ConceptKnowledge(2L, "오답", KnowledgeStatus.LEARNING, 0.0, 25, 1, null);

        TopicKnowledgeSummary summary = new TopicKnowledgeSummary(List.of(unknown, incorrect));

        assertThat(summary.status()).isEqualTo(KnowledgeStatus.LEARNING);
        assertThat(summary.masteryScore()).isZero();
        assertThat(summary.unknownCount()).isEqualTo(1);
        assertThat(summary.learningCount()).isEqualTo(1);
        assertThat(summary.stableCount()).isZero();
        assertThat(summary.concepts().getFirst().masteryScore()).isNull();
        assertThat(summary.concepts().getLast().masteryScore()).isZero();
    }

    @Test
    @DisplayName("개념이 없는 주제는 미평가이고 숙련도는 없으며 신뢰도는 0이다")
    void representsEmptyTopicAsUnknown() {
        TopicKnowledgeSummary summary = new TopicKnowledgeSummary(List.of());

        assertThat(summary.status()).isEqualTo(KnowledgeStatus.UNKNOWN);
        assertThat(summary.masteryScore()).isNull();
        assertThat(summary.confidenceScore()).isZero();
        assertThat(summary.unknownCount()).isZero();
        assertThat(summary.learningCount()).isZero();
        assertThat(summary.stableCount()).isZero();
    }

    @Test
    @DisplayName("모든 개념이 미평가이면 주제도 미평가이고 숙련도를 계산하지 않는다")
    void representsEntirelyUnassessedTopicAsUnknown() {
        ConceptKnowledge first = new ConceptKnowledge(1L, "스레드", KnowledgeStatus.UNKNOWN, null, 0, 0, null);
        ConceptKnowledge second = new ConceptKnowledge(2L, "프로세스", KnowledgeStatus.UNKNOWN, null, 0, 0, null);

        TopicKnowledgeSummary summary = new TopicKnowledgeSummary(List.of(first, second));

        assertThat(summary.status()).isEqualTo(KnowledgeStatus.UNKNOWN);
        assertThat(summary.masteryScore()).isNull();
        assertThat(summary.confidenceScore()).isZero();
        assertThat(summary.unknownCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("숙련도 평균은 평가된 개념만 사용하고 신뢰도 평균은 미평가 개념도 포함한다")
    void usesAssessedMasteryAndAllConceptConfidence() {
        ConceptKnowledge stable = new ConceptKnowledge(1L, "스레드", KnowledgeStatus.STABLE, 100.0, 75, 3, null);
        ConceptKnowledge incorrect = new ConceptKnowledge(2L, "프로세스", KnowledgeStatus.LEARNING, 0.0, 25, 1, null);
        ConceptKnowledge unknown = new ConceptKnowledge(3L, "스케줄링", KnowledgeStatus.UNKNOWN, null, 0, 0, null);
        ConceptKnowledge anotherUnknown = new ConceptKnowledge(4L, "동기화", KnowledgeStatus.UNKNOWN, null, 0, 0, null);

        TopicKnowledgeSummary summary = new TopicKnowledgeSummary(List.of(stable, incorrect, unknown, anotherUnknown));

        assertThat(summary.masteryScore()).isEqualTo(50.0);
        assertThat(summary.confidenceScore()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("모든 개념이 안정 상태일 때만 주제도 안정 상태다")
    void requiresEveryConceptToBeStable() {
        ConceptKnowledge stable = new ConceptKnowledge(1L, "스레드", KnowledgeStatus.STABLE, 100.0, 75, 3, null);
        ConceptKnowledge unknown = new ConceptKnowledge(2L, "프로세스", KnowledgeStatus.UNKNOWN, null, 0, 0, null);

        TopicKnowledgeSummary allStable = new TopicKnowledgeSummary(List.of(stable));
        TopicKnowledgeSummary partlyStable = new TopicKnowledgeSummary(List.of(stable, unknown));

        assertThat(allStable.status()).isEqualTo(KnowledgeStatus.STABLE);
        assertThat(allStable.stableCount()).isEqualTo(1);
        assertThat(partlyStable.status()).isEqualTo(KnowledgeStatus.LEARNING);
        assertThat(partlyStable.stableCount()).isEqualTo(1);
        assertThat(partlyStable.unknownCount()).isEqualTo(1);
    }
}
