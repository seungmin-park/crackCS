package com.example.crackcs.auth.service;

import com.example.crackcs.auth.repository.AuthenticationRequestBucketRepository;
import com.example.crackcs.exception.TooManyAuthenticationRequestsException;
import com.example.crackcs.support.ConcurrentRequests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"crackcs.auth.requests.login-source-limit=2",
        "crackcs.auth.requests.login-global-limit=3", "crackcs.auth.requests.max-state-count=4"})
@Import(LoginAttemptServiceTest.MutableClockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticationRequestLimitServiceTest {
    @Autowired AuthenticationRequestLimitService authenticationRequestLimitService;
    @Autowired AuthenticationRequestBucketRepository authenticationRequestBucketRepository;
    @Autowired LoginAttemptServiceTest.MutableClock clock;
    @Autowired AutowireCapableBeanFactory beanFactory;

    @AfterEach
    void cleanUp() {
        authenticationRequestBucketRepository.deleteAllInBatch();
        clock.advance(Duration.between(clock.instant(), Instant.parse("2026-08-31T00:00:00Z")));
    }

    @Test
    @DisplayName("서로 다른 서비스 인스턴스의 동시 요청도 주소 한도를 넘지 않는다")
    void sharesAtomicQuotaAcrossInstances() throws Exception {
        AuthenticationRequestLimitService otherInstance = beanFactory.createBean(DefaultAuthenticationRequestLimitService.class);
        List<Callable<Boolean>> requests = new ArrayList<>();
        for (int requestIndex = 0; requestIndex < 10; requestIndex++) {
            AuthenticationRequestLimitService target = requestIndex % 2 == 0 ? authenticationRequestLimitService : otherInstance;
            requests.add(() -> {
                try { target.reserveLoginRequest("192.0.2.1"); return true; }
                catch (TooManyAuthenticationRequestsException rejected) { return false; }
            });
        }

        List<Boolean> results = ConcurrentRequests.run(requests);

        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(2);
        assertThat(authenticationRequestBucketRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("한도에 도달한 주소는 요청 창이 만료되면 다시 허용된다")
    void restoresQuotaAfterWindowExpires() {
        authenticationRequestLimitService.reserveLoginRequest("192.0.2.1");
        authenticationRequestLimitService.reserveLoginRequest("192.0.2.1");
        assertThatThrownBy(() -> authenticationRequestLimitService.reserveLoginRequest("192.0.2.1"))
                .isInstanceOf(TooManyAuthenticationRequestsException.class);

        clock.advance(Duration.ofMinutes(1));

        assertThatCode(() -> authenticationRequestLimitService.reserveLoginRequest("192.0.2.1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("주소를 계속 교체해도 저장 상한을 넘지 않고 만료 상태를 정리한 뒤 용량을 재사용한다")
    void boundsSourceStateAndReclaimsExpiredCapacity() {
        authenticationRequestLimitService.reserveLoginRequest("192.0.2.1");
        authenticationRequestLimitService.reserveLoginRequest("192.0.2.2");
        assertThatThrownBy(() -> authenticationRequestLimitService.reserveLoginRequest("192.0.2.3"))
                .isInstanceOf(TooManyAuthenticationRequestsException.class);
        assertThat(authenticationRequestBucketRepository.count()).isEqualTo(4);

        clock.advance(Duration.ofMinutes(2).plusMillis(1));

        assertThatCode(() -> authenticationRequestLimitService.reserveLoginRequest("192.0.2.3"))
                .doesNotThrowAnyException();
        assertThat(authenticationRequestBucketRepository.count()).isEqualTo(3);
    }
}
