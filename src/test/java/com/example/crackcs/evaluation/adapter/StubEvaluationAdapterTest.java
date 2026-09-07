package com.example.crackcs.evaluation.adapter;

import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubEvaluationAdapterTest {

    private final EvaluationRequest request = new EvaluationRequest("문제", "모범 답안", "답변", List.of(1L, 2L));

    @Test
    @DisplayName("설정이 없으면 모든 Concept을 정답으로 평가한다")
    void defaultsToCorrect() {
        var result = new StubEvaluationAdapter("CORRECT").evaluate(request);

        assertThat(result.verdict()).isEqualTo(Verdict.CORRECT);
        assertThat(result.concepts()).extracting("conceptId").containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("서버 설정으로 검토 필요 결과를 재현한다")
    void producesConfiguredNeedsReview() {
        var result = new StubEvaluationAdapter("NEEDS_REVIEW").evaluate(request);

        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
        assertThat(result.concepts()).allMatch(concept -> concept.verdict() == Verdict.NEEDS_REVIEW);
    }

    @Test
    @DisplayName("서버 설정으로 시간 초과와 외부 실패를 재현한다")
    void producesConfiguredFailures() {
        assertThatThrownBy(() -> new StubEvaluationAdapter("TIMEOUT").evaluate(request))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("timeout");
        assertThatThrownBy(() -> new StubEvaluationAdapter("FAILURE").evaluate(request))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("failure");
    }

    @Test
    @DisplayName("스텁은 local 또는 test 단독 프로필에서만 등록된다")
    void profileExpressionPreventsProductionAndMixedActivation() {
        assertThat(stubBeanCount("local")).isOne();
        assertThat(stubBeanCount("test")).isOne();
        assertThat(stubBeanCount()).isZero();
        assertThat(stubBeanCount("prod")).isZero();
        assertThat(stubBeanCount("production")).isZero();
        assertThat(stubBeanCount("local", "prod")).isZero();
        assertThat(stubBeanCount("test", "production")).isZero();
    }

    private int stubBeanCount(String... profiles) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profiles);
            context.register(StubEvaluationAdapter.class);
            context.refresh();
            return context.getBeansOfType(StubEvaluationAdapter.class).size();
        }
    }
}
