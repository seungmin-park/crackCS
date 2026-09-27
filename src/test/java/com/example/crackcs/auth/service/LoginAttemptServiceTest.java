package com.example.crackcs.auth.service;

import com.example.crackcs.exception.TooManyLoginAttemptsException;
import com.example.crackcs.auth.repository.LoginAttemptRepository;
import com.example.crackcs.auth.domain.LoginAttempt;
import com.example.crackcs.support.ConcurrentRequests;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(LoginAttemptServiceTest.MutableClockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginAttemptServiceTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-08-31T00:00:00Z");

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private MutableClock clock;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @AfterEach
    void cleanUp() {
        loginAttemptRepository.deleteAll();
        clock.instant = INITIAL_TIME;
    }

    @Test
    @DisplayName("동시 최초 실패 다섯 건은 하나의 공유 기록으로 누적되어 차단된다")
    void serializesConcurrentFirstFailures() throws Exception {
        LoginAttemptService otherInstance = beanFactory.createBean(DefaultLoginAttemptService.class);
        List<Callable<Boolean>> failures = new ArrayList<>();
        for (int requestIndex = 0; requestIndex < 5; requestIndex++) {
            LoginAttemptService target = requestIndex % 2 == 0 ? loginAttemptService : otherInstance;
            failures.add(() -> { target.recordFailure("race@example.com", "127.0.0.1"); return true; });
        }

        assertThat(ConcurrentRequests.run(failures)).hasSize(5).containsOnly(true);
        assertThat(loginAttemptRepository.count()).isEqualTo(1);
        assertThatThrownBy(() -> otherInstance.checkAllowed("race@example.com", "127.0.0.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    @DisplayName("로그인 ID의 대소문자와 주변 공백을 바꿔도 실패 제한을 우회하지 못한다")
    void normalizesLoginKey() {
        for (int failure = 0; failure < 5; failure++) loginAttemptService.recordFailure(" Case@Example.com ", "127.0.0.1");
        assertThatThrownBy(() -> loginAttemptService.checkAllowed("case@example.com", "127.0.0.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        LoginAttempt persisted = loginAttemptRepository.findAll().getFirst();
        assertThat(persisted.getAttemptKey()).matches("[0-9a-f]{64}");
        assertThat(persisted.getAttemptKey()).doesNotContain("example", "127.0.0.1");
    }

    @Test
    @DisplayName("한 주소의 실패 기록은 다른 주소의 동일 계정까지 차단하지 않는다")
    void isolatesRemoteAddress() {
        for (int failure = 0; failure < 5; failure++) loginAttemptService.recordFailure("source@example.com", "127.0.0.1");
        assertThatCode(() -> loginAttemptService.checkAllowed("source@example.com", "127.0.0.2"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("로그인 성공은 같은 키의 실패 기록을 DB에서 제거한다")
    void clearsSuccessfulKey() {
        loginAttemptService.recordFailure("success@example.com", "127.0.0.1");
        loginAttemptService.recordSuccess("success@example.com", "127.0.0.1");
        assertThat(loginAttemptRepository.count()).isZero();
        LoginAttemptService otherInstance = beanFactory.createBean(DefaultLoginAttemptService.class);
        assertThatCode(() -> otherInstance.checkAllowed("success@example.com", "127.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("만료 기록 정리는 유효한 실패와 차단 기록을 유지한다")
    void purgesOnlyExpiredRecords() {
        loginAttemptService.recordFailure("expired@example.com", "127.0.0.1");
        for (int failure = 0; failure < 5; failure++) loginAttemptService.recordFailure("blocked@example.com", "127.0.0.1");
        clock.advance(Duration.ofMinutes(10).plusMillis(1));
        loginAttemptService.recordFailure("active@example.com", "127.0.0.1");

        loginAttemptService.purgeExpiredAttempts();

        assertThat(loginAttemptRepository.count()).isEqualTo(2);
        assertThatThrownBy(() -> loginAttemptService.checkAllowed("blocked@example.com", "127.0.0.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        clock.advance(Duration.ofMinutes(5));
        loginAttemptService.purgeExpiredAttempts();
        assertThat(loginAttemptRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 서비스 인스턴스에서도 같은 계정과 주소의 차단 기록을 공유한다")
    void sharesBlockAcrossServiceInstances() {
        for (int attempt = 0; attempt < 5; attempt++) {
            loginAttemptService.recordFailure("shared@example.com", "127.0.0.1");
        }
        LoginAttemptService otherInstance = beanFactory.createBean(DefaultLoginAttemptService.class);

        assertThatThrownBy(() -> otherInstance.checkAllowed("shared@example.com", "127.0.0.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
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

    @Test
    @DisplayName("사전 확인 뒤 차단 기간이 만료되어도 실패 기록은 새 창에서 시작한다")
    void recordsFailureAfterBlockExpiresWithoutPrecheck() {
        for (int attempt = 0; attempt < 5; attempt++) {
            loginAttemptService.recordFailure("expiry@example.com", "127.0.0.1");
        }
        clock.advance(Duration.ofMinutes(15));

        assertThatCode(() -> loginAttemptService.recordFailure("expiry@example.com", "127.0.0.1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> loginAttemptService.checkAllowed("expiry@example.com", "127.0.0.1"))
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

        private Instant instant = INITIAL_TIME;

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
