package com.example.crackcs.learning.followup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;
import com.example.crackcs.learning.followup.service.FollowUpQuestionResult.QuestionResult;
import com.example.crackcs.learning.followup.service.FollowUpQuestionResult.TopicResult;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FollowUpQuestionResultTest {
    @ParameterizedTest
    @MethodSource("invalidStates")
    @DisplayName("상태와 사유 및 질문의 모순된 조합을 거부한다")
    void rejectsInconsistentState(FollowUpStatus status, FollowUpReason reason, QuestionResult question) {
        assertThatThrownBy(() -> new FollowUpQuestionResult(status, reason, question))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> invalidStates() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of(FollowUpStatus.READY, null, null),
                Arguments.of(FollowUpStatus.READY, FollowUpReason.PROVIDER_ERROR, question()),
                Arguments.of(FollowUpStatus.FAILED, null, null),
                Arguments.of(FollowUpStatus.UNAVAILABLE, null, null),
                Arguments.of(FollowUpStatus.PENDING, null, question()),
                Arguments.of(FollowUpStatus.PROCESSING, null, question()),
                Arguments.of(FollowUpStatus.FAILED, FollowUpReason.PROVIDER_ERROR, question()),
                Arguments.of(FollowUpStatus.UNAVAILABLE, FollowUpReason.CONTENT_UNAVAILABLE, question()));
    }

    @Test
    @DisplayName("재시도 대기 결과는 직전 실패 사유를 유지할 수 있다")
    void preservesPreviousFailureDuringRetry() {
        FollowUpQuestionResult result = FollowUpQuestionResult.withoutQuestion(
                FollowUpStatus.PENDING, FollowUpReason.PROVIDER_TIMEOUT);

        assertThat(result.reason()).isEqualTo(FollowUpReason.PROVIDER_TIMEOUT);
        assertThat(result.question()).isNull();
    }

    private static QuestionResult question() {
        return new QuestionResult(1L, new TopicResult(1L, "OS", "운영체제"),
                QuestionDifficulty.BASIC, "후속 질문");
    }
}
