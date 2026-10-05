package com.example.crackcs.auth.service;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.domain.LoginAttempt;
import com.example.crackcs.auth.domain.AuthenticationRequestBucket;
import com.example.crackcs.auth.repository.AuthenticationRequestBucketRepository;
import com.example.crackcs.exception.TooManyAuthenticationRequestsException;
import org.springframework.beans.factory.annotation.Value;
import com.example.crackcs.auth.repository.LoginAttemptRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DefaultLoginAttemptService implements LoginAttemptService {
    private static final int MAX_CREATE_ATTEMPTS = 3;
    private final Clock clock;
    private final LoginAttemptRepository loginAttemptRepository;
    private final TransactionTemplate attemptTransaction;
    private final AuthenticationRequestLimitService authenticationRequestLimitService;
    private final AuthenticationRequestBucketRepository authenticationRequestBucketRepository;
    private final int maxStateCount;

    public DefaultLoginAttemptService(Clock clock, LoginAttemptRepository loginAttemptRepository,
                                      PlatformTransactionManager transactionManager,
                                      AuthenticationRequestLimitService authenticationRequestLimitService,
                                      AuthenticationRequestBucketRepository authenticationRequestBucketRepository,
                                      @Value("${crackcs.auth.requests.max-state-count:5000}") int maxStateCount) {
        this.clock = clock;
        this.loginAttemptRepository = loginAttemptRepository;
        this.authenticationRequestLimitService = authenticationRequestLimitService;
        this.authenticationRequestBucketRepository = authenticationRequestBucketRepository;
        this.maxStateCount = maxStateCount;
        this.attemptTransaction = new TransactionTemplate(transactionManager);
        this.attemptTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void checkAllowed(String loginId, String remoteAddress) {
        authenticationRequestLimitService.reserveLoginRequest(remoteAddress);
        String attemptKey = key(loginId);
        attemptTransaction.executeWithoutResult(status -> {
            Optional<LoginAttempt> attempt = loginAttemptRepository.findByAttemptKey(attemptKey);
            if (attempt.isPresent()) attempt.get().ensureAllowed(clock.instant());
            else if (loginAttemptRepository.count() >= maxStateCount) throw new TooManyAuthenticationRequestsException(60);
        });
    }

    @Override
    public void recordFailure(String loginId, String remoteAddress) {
        String attemptKey = key(loginId);
        for (int attemptNumber = 1; ; attemptNumber++) {
            try {
                attemptTransaction.executeWithoutResult(status -> {
                    authenticationRequestBucketRepository.findByBucketKeyForUpdate(AuthenticationRequestBucket.CAPACITY_KEY)
                            .orElseGet(() -> authenticationRequestBucketRepository.saveAndFlush(AuthenticationRequestBucket.builder()
                                    .bucketKey(AuthenticationRequestBucket.CAPACITY_KEY).now(clock.instant()).build()));
                    LoginAttempt attempt = loginAttemptRepository.findByAttemptKeyForUpdate(attemptKey)
                            .orElseGet(() -> createAttempt(attemptKey, clock.instant()));
                    attempt.recordFailure(clock.instant());
                });
                return;
            } catch (DataIntegrityViolationException conflict) {
                if (attemptNumber == MAX_CREATE_ATTEMPTS || !AuthenticationThrottleConflicts.isUniqueKeyConflict(conflict)) throw conflict;
                // A concurrent first failure inserted the same key. Retry after this transaction rolled back.
            }
        }
    }

    private LoginAttempt createAttempt(String attemptKey, Instant now) {
        if (loginAttemptRepository.count() >= maxStateCount) throw new TooManyAuthenticationRequestsException(60);
        // Flush detects the unique-key race inside this independent transaction, before recording a failure.
        return loginAttemptRepository.saveAndFlush(LoginAttempt.builder().attemptKey(attemptKey).now(now).build());
    }

    @Override
    public void recordSuccess(String loginId, String remoteAddress) {
        String attemptKey = key(loginId);
        attemptTransaction.executeWithoutResult(status -> loginAttemptRepository.findByAttemptKeyForUpdate(attemptKey)
                .ifPresent(loginAttemptRepository::delete));
    }

    @Override
    @Scheduled(fixedDelayString = "${crackcs.auth.login-attempt-cleanup-interval:1m}")
    public void purgeExpiredAttempts() {
        Instant now = clock.instant();
        for (int page = 0; page <= maxStateCount / 200; page++) {
            List<String> expiredKeys = loginAttemptRepository.findExpiredKeys(now.minus(LoginAttempt.FAILURE_WINDOW), now, PageRequest.of(0, 200));
            if (expiredKeys.isEmpty()) return;
            for (String attemptKey : expiredKeys) {
                attemptTransaction.executeWithoutResult(status -> {
                    Optional<LoginAttempt> attempt = loginAttemptRepository.findByAttemptKeyForUpdate(attemptKey);
                    if (attempt.isPresent() && attempt.get().isExpiredAt(clock.instant())) {
                        loginAttemptRepository.delete(attempt.get());
                    }
                });
            }
        }
    }

    private String key(String loginId) {
        String normalizedLoginId = AuthAccount.normalizeLoginId(loginId);
        String source = "account|" + normalizedLoginId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

}
