package com.example.crackcs.auth.service;

import com.example.crackcs.auth.domain.AuthenticationRequestBucket;
import com.example.crackcs.auth.repository.AuthenticationRequestBucketRepository;
import com.example.crackcs.exception.TooManyAuthenticationRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DefaultAuthenticationRequestLimitService implements AuthenticationRequestLimitService {
    private static final List<String> GUARD_KEYS = List.of(AuthenticationRequestBucket.CAPACITY_KEY,
            AuthenticationRequestBucket.LOGIN_GLOBAL_KEY, AuthenticationRequestBucket.SIGN_UP_GLOBAL_KEY);
    private final AuthenticationRequestBucketRepository authenticationRequestBucketRepository;
    private final Clock clock;
    private final TransactionTemplate requestTransaction;
    private final int loginSourceLimit;
    private final int loginGlobalLimit;
    private final int signUpSourceLimit;
    private final int signUpGlobalLimit;
    private final int maxStateCount;

    public DefaultAuthenticationRequestLimitService(AuthenticationRequestBucketRepository authenticationRequestBucketRepository, Clock clock,
            PlatformTransactionManager transactionManager,
            @Value("${crackcs.auth.requests.login-source-limit:30}") int loginSourceLimit,
            @Value("${crackcs.auth.requests.login-global-limit:300}") int loginGlobalLimit,
            @Value("${crackcs.auth.requests.sign-up-source-limit:5}") int signUpSourceLimit,
            @Value("${crackcs.auth.requests.sign-up-global-limit:30}") int signUpGlobalLimit,
            @Value("${crackcs.auth.requests.max-state-count:5000}") int maxStateCount) {
        if (loginSourceLimit < 1 || loginGlobalLimit < 1 || signUpSourceLimit < 1 || signUpGlobalLimit < 1 || maxStateCount < 3) {
            throw new IllegalArgumentException("request limits must be positive and state capacity at least three");
        }
        this.authenticationRequestBucketRepository = authenticationRequestBucketRepository;
        this.clock = clock;
        this.requestTransaction = new TransactionTemplate(transactionManager);
        this.requestTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.loginSourceLimit = loginSourceLimit;
        this.loginGlobalLimit = loginGlobalLimit;
        this.signUpSourceLimit = signUpSourceLimit;
        this.signUpGlobalLimit = signUpGlobalLimit;
        this.maxStateCount = maxStateCount;
    }

    @Override
    public void reserveLoginRequest(String remoteAddress) {
        reserve("login", AuthenticationRequestBucket.LOGIN_GLOBAL_KEY, remoteAddress, loginSourceLimit, loginGlobalLimit);
    }

    @Override
    public void reserveSignUpRequest(String remoteAddress) {
        reserve("sign-up", AuthenticationRequestBucket.SIGN_UP_GLOBAL_KEY, remoteAddress, signUpSourceLimit, signUpGlobalLimit);
    }

    private void reserve(String operation, String globalKey, String remoteAddress, int sourceLimit, int globalLimit) {
        for (int attempt = 1; ; attempt++) {
            try {
                requestTransaction.executeWithoutResult(status -> {
                    Instant now = clock.instant();
                    lockCapacityGuard(now);
                    bucket(globalKey, now).reserve(now, globalLimit);
                    bucket(AuthenticationRequestBucket.sourceKey(operation, remoteAddress), now).reserve(now, sourceLimit);
                });
                return;
            } catch (DataIntegrityViolationException conflict) {
                if (attempt == 3 || !AuthenticationThrottleConflicts.isUniqueKeyConflict(conflict)) throw conflict;
            }
        }
    }

    private void lockCapacityGuard(Instant now) {
        authenticationRequestBucketRepository.findByBucketKeyForUpdate(AuthenticationRequestBucket.CAPACITY_KEY)
                .orElseGet(() -> authenticationRequestBucketRepository.saveAndFlush(AuthenticationRequestBucket.builder()
                        .bucketKey(AuthenticationRequestBucket.CAPACITY_KEY).now(now).build()));
    }

    private AuthenticationRequestBucket bucket(String key, Instant now) {
        return authenticationRequestBucketRepository.findByBucketKeyForUpdate(key).orElseGet(() -> {
            if (authenticationRequestBucketRepository.count() >= maxStateCount) {
                authenticationRequestBucketRepository.deleteExpiredSources(now.minus(AuthenticationRequestBucket.WINDOW.multipliedBy(2)), GUARD_KEYS);
                if (authenticationRequestBucketRepository.count() >= maxStateCount) throw new TooManyAuthenticationRequestsException(60);
            }
            return authenticationRequestBucketRepository.save(AuthenticationRequestBucket.builder().bucketKey(key).now(now).build());
        });
    }

    @Scheduled(fixedDelayString = "${crackcs.auth.request-cleanup-interval:1m}")
    public void purgeExpiredRequestSources() {
        requestTransaction.executeWithoutResult(status -> {
            Instant now = clock.instant();
            lockCapacityGuard(now);
            authenticationRequestBucketRepository.deleteExpiredSources(now.minus(AuthenticationRequestBucket.WINDOW.multipliedBy(2)), GUARD_KEYS);
        });
    }
}
