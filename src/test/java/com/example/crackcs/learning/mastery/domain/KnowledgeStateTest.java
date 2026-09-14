package com.example.crackcs.learning.mastery.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class KnowledgeStateTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 13, 12, 0);

    @Test
    @DisplayName("회원과 개념 연관관계 없이 학습 상태를 생성할 수 없다")
    void rejectsMissingAssociations() {
        assertThatIllegalArgumentException().isThrownBy(() -> KnowledgeState.builder().build());
    }

    @Test
    @DisplayName("평가 개념 연관관계 없이 적용 기록을 생성할 수 없다")
    void rejectsMissingEvaluationConcept() {
        assertThatIllegalArgumentException().isThrownBy(() -> AppliedEvaluationConcept.builder().build());
    }

    @Test
    @DisplayName("미평가 상태는 점수 없음과 신뢰도 0으로 시작한다")
    void startsUnknown() {
        KnowledgeState state = state();
        assertThat(state.getStatus()).isEqualTo(KnowledgeStatus.UNKNOWN);
        assertThat(state.getMasteryScore()).isNull();
        assertThat(state.getConfidenceScore()).isZero();
        assertThat(state.getAttemptCount()).isZero();
        assertThat(state.getAlgorithmVersion()).isEqualTo("knowledge-v1");
        assertThat(state.getCreatedAt()).isNotNull().isEqualTo(state.getUpdatedAt());
    }

    @Test
    @DisplayName("오답의 숙련도 0은 미평가와 구분한다")
    void distinguishesZeroFromUnknown() {
        KnowledgeState state = state();
        state.observe(1L, Verdict.INCORRECT, now);
        assertThat(state.getMasteryScore()).isZero();
        assertThat(state.getStatus()).isEqualTo(KnowledgeStatus.LEARNING);
        assertThat(state.getConfidenceScore()).isEqualTo(25);
    }

    @Test
    @DisplayName("정답 세 번이면 안정 상태가 된다")
    void becomesStableAfterThreeCorrectObservations() {
        KnowledgeState state = state();
        state.observe(1L, Verdict.CORRECT, now);
        state.observe(2L, Verdict.CORRECT, now.plusMinutes(1));
        state.observe(3L, Verdict.CORRECT, now.plusMinutes(2));
        assertThat(state.getMasteryScore()).isEqualTo(100);
        assertThat(state.getConfidenceScore()).isEqualTo(75);
        assertThat(state.getStatus()).isEqualTo(KnowledgeStatus.STABLE);
    }

    @Test
    @DisplayName("안정 상태 이후 오답은 최신 관측을 두 번 반영하여 숙련도 60으로 낮춘다")
    void regressesAfterIncorrectObservation() {
        KnowledgeState state = state();
        state.observe(1L, Verdict.CORRECT, now);
        state.observe(2L, Verdict.CORRECT, now.plusMinutes(1));
        state.observe(3L, Verdict.CORRECT, now.plusMinutes(2));
        state.observe(4L, Verdict.INCORRECT, now.plusMinutes(3));
        assertThat(state.getMasteryScore()).isEqualTo(60);
        assertThat(state.getConfidenceScore()).isEqualTo(100);
        assertThat(state.getStatus()).isEqualTo(KnowledgeStatus.LEARNING);
    }

    @Test
    @DisplayName("도착 순서와 관계없이 평가 시각과 식별자로 최신 점수를 선택한다")
    void ordersByEvaluationTimeThenId() {
        KnowledgeState state = state();
        state.observe(10L, Verdict.INCORRECT, now);
        state.observe(12L, Verdict.CORRECT, now.minusMinutes(1));
        state.observe(9L, Verdict.CORRECT, now);
        assertThat(state.getMasteryScore()).isEqualTo(50);
        assertThat(state.getLatestEvaluationConceptId()).isEqualTo(10L);
        state.observe(11L, Verdict.PARTIALLY_CORRECT, now);
        assertThat(state.getMasteryScore()).isEqualTo(60);
        assertThat(state.getLastEvaluatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("유효하지 않은 관측은 상태와 시각을 바꾸지 않는다")
    void rejectsInvalidObservationAtomically() {
        KnowledgeState state = state();
        state.observe(1L, Verdict.CORRECT, now);
        LocalDateTime updatedAt = state.getUpdatedAt();
        assertThatIllegalArgumentException().isThrownBy(() -> state.observe(2L, Verdict.NEEDS_REVIEW, now));
        assertThatIllegalArgumentException().isThrownBy(() -> state.observe(0L, Verdict.CORRECT, now));
        assertThatIllegalArgumentException().isThrownBy(() -> state.observe(2L, null, now));
        assertThatIllegalArgumentException().isThrownBy(() -> state.observe(2L, Verdict.CORRECT, null));
        assertThat(state.getAttemptCount()).isEqualTo(1);
        assertThat(state.getMasteryScore()).isEqualTo(100);
        assertThat(state.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("회원과 개념은 모두 필수다")
    void rejectsInvalidIdentity() {
        assertThatIllegalArgumentException().isThrownBy(() -> KnowledgeState.builder().build());
        assertThatIllegalArgumentException().isThrownBy(() -> KnowledgeState.builder().member(member()).build());
    }

    private Member member() {
        return Member.builder().nickname("회원").build();
    }

    private Concept concept() {
        return Concept.builder().topic(Topic.builder().code("OS").name("운영체제").build()).code("THREAD").name("스레드")
                .build();
    }

    private KnowledgeState state() {
        return KnowledgeState.builder().member(member()).concept(concept()).build();
    }
}
