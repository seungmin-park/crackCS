package com.example.crackcs.auth.service;

import com.example.crackcs.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(LoginAttemptServiceTest.MutableClockConfiguration.class)
class LoginAttemptServiceTest {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.set(Instant.parse("2026-08-31T00:00:00Z"));
        loginAttemptService.recordSuccess("limit@example.com", "127.0.0.1");
    }

    @Test
    @DisplayName("10분 안에 로그인에 5회 실패하면 같은 계정과 주소를 15분간 차단한다")
    void blocksRepeatedLoginFailures() {
        for (int attempt = 0; attempt < 5; attempt++) {
            loginAttemptService.recordFailure("limit@example.com", "127.0.0.1");
        }

        assertThatThrownBy(() -> loginAttemptService.checkAllowed("limit@example.com", "127.0.0.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class)
                .hasMessage("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.");
    }

    @Test
    @DisplayName("차단 시간이 지나면 로그인을 다시 시도할 수 있다")
    void allowsLoginAfterBlockExpires() {
        for (int attempt = 0; attempt < 5; attempt++) {
            loginAttemptService.recordFailure("limit@example.com", "127.0.0.1");
        }
        clock.advance(Duration.ofMinutes(15));

        assertThatCode(() -> loginAttemptService.checkAllowed("limit@example.com", "127.0.0.1"))
                .doesNotThrowAnyException();
    }

    @TestConfiguration
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    static class MutableClock extends Clock {

        private Instant instant = Instant.EPOCH;

        void set(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
