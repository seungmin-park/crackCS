package com.example.crackcs.auth.domain;

import com.example.crackcs.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class LoginAttemptTest {
    private static final Instant START = Instant.parse("2026-09-27T00:00:00Z");
    private static final String KEY = "a".repeat(64);

    @Test
    @DisplayName("생성 시 생성과 수정 시각은 같고 실패 기록은 비어 있다")
    void initializesEmptyAttempt() {
        LoginAttempt attempt = LoginAttempt.builder().attemptKey(KEY).now(START).build();
        assertThat(attempt.getCreatedAt()).isEqualTo(START);
        assertThat(attempt.getUpdatedAt()).isEqualTo(START);
        assertThat(attempt.getFailures()).isEmpty();
    }

    @Test
    @DisplayName("로그인 식별자의 원문 대신 SHA256 형식의 키만 저장할 수 있다")
    void rejectsPlaintextKey() {
        assertThatThrownBy(() -> LoginAttempt.builder().attemptKey("person@example.com|127.0.0.1").now(START).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("잘못된 기록 시각은 실패 목록과 수정 시각을 바꾸지 않는다")
    void rejectsNullTimeWithoutMutation() {
        LoginAttempt attempt = LoginAttempt.builder().attemptKey(KEY).now(START).build();
        attempt.recordFailure(START);
        assertThatThrownBy(() -> attempt.recordFailure(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(attempt.getFailures()).containsExactly(START);
        assertThat(attempt.getUpdatedAt()).isEqualTo(START);
    }

    @Test
    @DisplayName("실패 시각이 정확히 10분 전이면 현재 실패 창에 포함한다")
    void includesFailureAtWindowBoundary() {
        LoginAttempt attempt = LoginAttempt.builder().attemptKey(KEY).now(START).build();
        for (int failure = 0; failure < 4; failure++) attempt.recordFailure(START);
        Instant boundary = START.plus(Duration.ofMinutes(10));
        attempt.recordFailure(boundary);
        assertThatThrownBy(() -> attempt.ensureAllowed(boundary)).isInstanceOf(TooManyLoginAttemptsException.class);
        assertThat(attempt.getCreatedAt()).isEqualTo(START);
        assertThat(attempt.getUpdatedAt()).isEqualTo(boundary);
    }

    @Test
    @DisplayName("10분을 넘긴 실패는 새 실패 횟수에 포함하지 않는다")
    void discardsFailuresOutsideWindow() {
        LoginAttempt attempt = LoginAttempt.builder().attemptKey(KEY).now(START).build();
        for (int failure = 0; failure < 4; failure++) attempt.recordFailure(START);
        Instant later = START.plus(Duration.ofMinutes(10)).plusMillis(1);
        attempt.recordFailure(later);
        assertThat(attempt.getFailures()).containsExactly(later);
        assertThatCode(() -> attempt.ensureAllowed(later)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("차단 중 늦게 도착한 실패는 기록을 늘리거나 차단 종료를 연장하지 않는다")
    void keepsBlockDeadlineForInFlightFailures() {
        LoginAttempt attempt = LoginAttempt.builder().attemptKey(KEY).now(START).build();
        for (int failure = 0; failure < 5; failure++) attempt.recordFailure(START);
        attempt.recordFailure(START.plusSeconds(2));
        assertThat(attempt.getFailures()).hasSize(5);
        assertThat(attempt.getBlockedUntil()).isEqualTo(START.plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("차단 종료 시각에는 이전 실패를 버리고 새 창을 시작한다")
    void restartsAtBlockDeadline() {
        LoginAttempt attempt = LoginAttempt.builder().attemptKey(KEY).now(START).build();
        for (int failure = 0; failure < 5; failure++) attempt.recordFailure(START);
        Instant deadline = START.plus(Duration.ofMinutes(15));
        assertThatCode(() -> attempt.ensureAllowed(deadline)).doesNotThrowAnyException();
        attempt.recordFailure(deadline);
        assertThat(attempt.getFailures()).containsExactly(deadline);
        assertThat(attempt.getBlockedUntil()).isNull();
    }

    @Test
    @DisplayName("외부에서 실패 시각 목록을 직접 수정할 수 없다")
    void protectsFailureHistory() {
        LoginAttempt attempt = LoginAttempt.builder().attemptKey(KEY).now(START).build();
        assertThatThrownBy(() -> attempt.getFailures().add(START)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("유효한 실패 창과 아직 끝나지 않은 차단 기록은 정리 대상이 아니다")
    void keepsActiveHistory() {
        LoginAttempt attempt = LoginAttempt.builder().attemptKey(KEY).now(START).build();
        attempt.recordFailure(START);
        assertThat(attempt.isExpiredAt(START.plus(Duration.ofMinutes(10)))).isFalse();
        assertThat(attempt.isExpiredAt(START.plus(Duration.ofMinutes(10)).plusMillis(1))).isTrue();
        for (int failure = 0; failure < 4; failure++) attempt.recordFailure(START);
        assertThat(attempt.isExpiredAt(START.plus(Duration.ofMinutes(14)))).isFalse();
        assertThat(attempt.isExpiredAt(START.plus(Duration.ofMinutes(15)))).isTrue();
    }
}
