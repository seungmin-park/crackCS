package com.example.crackcs.learning.followup.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import com.example.crackcs.learning.answer.domain.Answer;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FollowUpGenerationTest {
    @Test
    @DisplayName("lease 시각 계산이 실패하면 선점 상태를 부분 변경하지 않는다")
    void claimOverflowPreservesState() {
        FollowUpGeneration job = FollowUpGeneration.builder().answer(mock(Answer.class)).build();
        assertThatThrownBy(() -> job.claim("worker", LocalDateTime.MAX, Duration.ofSeconds(1)))
                .isInstanceOf(RuntimeException.class);
        assertThat(job.getStatus()).isEqualTo(FollowUpStatus.PENDING);
        assertThat(job.getAttemptCount()).isZero();
    }

    @Test
    @DisplayName("재시도 시각 계산이 실패하면 기존 lease를 보존한다")
    void retryOverflowPreservesState() {
        FollowUpGeneration job = FollowUpGeneration.builder().answer(mock(Answer.class)).build();
        LocalDateTime now = LocalDateTime.MAX.minusSeconds(2);
        assertThat(job.claim("worker", now, Duration.ofSeconds(1))).isTrue();
        assertThatThrownBy(() -> job.retry("worker", now, FollowUpReason.PROVIDER_TIMEOUT, Duration.ofSeconds(3)))
                .isInstanceOf(RuntimeException.class);
        assertThat(job.hasActiveLease("worker", now)).isTrue();
        assertThat(job.getReason()).isNull();
    }

    @Test
    @DisplayName("유효한 lease 동안에는 다른 작업자가 선점할 수 없다")
    void onlyOneLeaseOwnsWork() {
        FollowUpGeneration job = FollowUpGeneration.builder().answer(mock(Answer.class)).build();
        LocalDateTime now = LocalDateTime.now().plusSeconds(1);
        assertThat(job.claim("first", now, Duration.ofSeconds(10))).isTrue();
        assertThat(job.claim("second", now.plusSeconds(1), Duration.ofSeconds(10))).isFalse();
        assertThat(job.hasActiveLease("first", now.plusSeconds(1))).isTrue();
        assertThat(job.hasActiveLease("second", now.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("만료된 lease도 시도 횟수에 포함되어 네 번째 호출을 막는다")
    void expiredLeasesExhaustThreeAttempts() {
        FollowUpGeneration job = FollowUpGeneration.builder().answer(mock(Answer.class)).build();
        LocalDateTime now = LocalDateTime.now().plusSeconds(1);
        assertThat(job.claim("first", now, Duration.ofSeconds(1))).isTrue();
        assertThat(job.claim("second", now.plusSeconds(1), Duration.ofSeconds(1))).isTrue();
        assertThat(job.claim("third", now.plusSeconds(2), Duration.ofSeconds(1))).isTrue();
        assertThat(job.claim("fourth", now.plusSeconds(3), Duration.ofSeconds(1))).isFalse();
        assertThat(job.getStatus()).isEqualTo(FollowUpStatus.FAILED);
        assertThat(job.getReason()).isEqualTo(FollowUpReason.ATTEMPTS_EXHAUSTED);
        assertThat(job.getAttemptCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("재시도 시각 전에는 호출하지 않고 마지막 실패의 안전 코드만 보존한다")
    void retriesAreBoundedAndDelayed() {
        FollowUpGeneration job = FollowUpGeneration.builder().answer(mock(Answer.class)).build();
        LocalDateTime now = LocalDateTime.now().plusSeconds(1);
        assertThat(job.claim("first", now, Duration.ofMinutes(1))).isTrue();
        job.retry("first", now, FollowUpReason.PROVIDER_TIMEOUT, Duration.ofSeconds(5));
        assertThat(job.claim("early", now.plusSeconds(4), Duration.ofMinutes(1))).isFalse();
        assertThat(job.claim("second", now.plusSeconds(5), Duration.ofMinutes(1))).isTrue();
        assertThatThrownBy(() -> job.retry("first", now.plusSeconds(5), FollowUpReason.PROVIDER_ERROR, Duration.ZERO))
                .isInstanceOf(IllegalStateException.class);
        assertThat(job.getStatus()).isEqualTo(FollowUpStatus.PROCESSING);
    }
}
