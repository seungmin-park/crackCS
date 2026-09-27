package com.example.crackcs.auth.service;

import com.example.crackcs.auth.domain.LoginAttempt;
import com.example.crackcs.auth.repository.LoginAttemptRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "crackcs.evaluation.worker-enabled=false",
        "crackcs.followup.worker-enabled=false",
        "crackcs.auth.login-attempt-cleanup-interval=20ms"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginAttemptSchedulingTest {
    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @AfterEach
    void cleanUp() {
        loginAttemptRepository.deleteAll();
    }

    @Test
    @DisplayName("평가와 후속 Worker를 모두 꺼도 만료된 로그인 기록은 자동으로 정리된다")
    void purgesExpiredAttemptsWithoutEvaluationWorkers() {
        Instant expiredTime = Instant.now().minus(Duration.ofMinutes(20));
        LoginAttempt attempt = LoginAttempt.builder().attemptKey("a".repeat(64)).now(expiredTime).build();
        attempt.recordFailure(expiredTime);
        loginAttemptRepository.save(attempt);

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(loginAttemptRepository.count()).isZero());
    }
}
