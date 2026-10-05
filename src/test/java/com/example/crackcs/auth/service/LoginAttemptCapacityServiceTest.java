package com.example.crackcs.auth.service;

import com.example.crackcs.auth.repository.LoginAttemptRepository;
import com.example.crackcs.auth.repository.AuthenticationRequestBucketRepository;
import com.example.crackcs.exception.TooManyAuthenticationRequestsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "crackcs.auth.requests.max-state-count=3")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginAttemptCapacityServiceTest {
    @Autowired LoginAttemptService loginAttemptService;
    @Autowired LoginAttemptRepository loginAttemptRepository;
    @Autowired AuthenticationRequestBucketRepository authenticationRequestBucketRepository;

    @AfterEach
    void cleanUp() {
        loginAttemptRepository.deleteAll();
        authenticationRequestBucketRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("서로 다른 계정의 실패 저장도 최대 기록 수를 넘으면 새 행을 만들지 않는다")
    void boundsFailureStateCardinality() {
        for (int attempt = 0; attempt < 3; attempt++) {
            loginAttemptService.recordFailure("unknown" + attempt + "@example.com", "192.0.2.1");
        }

        assertThatThrownBy(() -> loginAttemptService.recordFailure("another@example.com", "192.0.2.2"))
                .isInstanceOf(TooManyAuthenticationRequestsException.class);

        assertThat(loginAttemptRepository.count()).isEqualTo(3);
    }
}
