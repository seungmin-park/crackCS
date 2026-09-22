package com.example.crackcs.learning.followup.service;

import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.exception.EvaluationTimeoutException;
import com.example.crackcs.learning.followup.domain.FollowUpGenerationResult;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.port.FollowUpQuestionGenerator;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FollowUpGenerationAttemptExecutorTest {

    @Test
    @DisplayName("생성 provider가 없으면 재시도 결과를 반환한다")
    void retriesWhenProviderIsUnavailable() {
        ObjectProvider<FollowUpQuestionGenerator> provider = provider(null);
        FollowUpGenerationAttemptExecutor executor = new FollowUpGenerationAttemptExecutor(provider);

        FollowUpGenerationAttemptOutcome outcome = executor.execute(request());

        assertThat(outcome).isEqualTo(
                new FollowUpGenerationAttemptOutcome.RetryRequired(FollowUpReason.PROVIDER_ERROR));
    }

    @Test
    @DisplayName("생성 시간이 초과되면 재시도 결과를 반환한다")
    void retriesAfterProviderTimeout() {
        ObjectProvider<FollowUpQuestionGenerator> provider = provider(request -> {
            throw new EvaluationTimeoutException();
        });
        FollowUpGenerationAttemptExecutor executor = new FollowUpGenerationAttemptExecutor(provider);

        FollowUpGenerationAttemptOutcome outcome = executor.execute(request());

        assertThat(outcome).isEqualTo(
                new FollowUpGenerationAttemptOutcome.RetryRequired(FollowUpReason.PROVIDER_TIMEOUT));
    }

    @Test
    @DisplayName("승인되지 않은 근거를 사용한 생성 결과는 영구 실패 결과를 반환한다")
    void failsGenerationResultWithUnapprovedEvidence() {
        ObjectProvider<FollowUpQuestionGenerator> provider = provider(request -> new FollowUpGenerationResult(
                "후속 질문", "정답", request.conceptId(), List.of(999L),
                "test-model", "follow-up-v1", 1L, 2L, 3L));
        FollowUpGenerationAttemptExecutor executor = new FollowUpGenerationAttemptExecutor(provider);

        FollowUpGenerationAttemptOutcome outcome = executor.execute(request());

        assertThat(outcome).isEqualTo(
                new FollowUpGenerationAttemptOutcome.FailureRequired(FollowUpReason.INVALID_RESULT));
    }

    @Test
    @DisplayName("선택되지 않은 개념을 사용한 생성 결과는 영구 실패 결과를 반환한다")
    void failsGenerationResultWithUnapprovedConcept() {
        ObjectProvider<FollowUpQuestionGenerator> provider = provider(request -> new FollowUpGenerationResult(
                "후속 질문", "정답", 12L, List.of(7L),
                "test-model", "follow-up-v1", 1L, 2L, 3L));
        FollowUpGenerationAttemptExecutor executor = new FollowUpGenerationAttemptExecutor(provider);

        FollowUpGenerationAttemptOutcome outcome = executor.execute(request());

        assertThat(outcome).isEqualTo(
                new FollowUpGenerationAttemptOutcome.FailureRequired(FollowUpReason.INVALID_RESULT));
    }

    @Test
    @DisplayName("생성 provider 오류는 재시도 결과를 반환한다")
    void retriesAfterProviderError() {
        ObjectProvider<FollowUpQuestionGenerator> provider = provider(request -> {
            throw new IllegalStateException("provider unavailable");
        });
        FollowUpGenerationAttemptExecutor executor = new FollowUpGenerationAttemptExecutor(provider);

        FollowUpGenerationAttemptOutcome outcome = executor.execute(request());

        assertThat(outcome).isEqualTo(
                new FollowUpGenerationAttemptOutcome.RetryRequired(FollowUpReason.PROVIDER_ERROR));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<FollowUpQuestionGenerator> provider(FollowUpQuestionGenerator generator) {
        ObjectProvider<FollowUpQuestionGenerator> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(generator);
        return provider;
    }

    private FollowUpRequest request() {
        return new FollowUpRequest(
                "원본 질문", 11L, "개념", Verdict.CORRECT, "피드백", List.of(), List.of(),
                List.of(new FollowUpRequest.Evidence(7L, "근거")));
    }
}
