package com.example.crackcs.auth.service;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.exception.TooManyLoginAttemptsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DefaultLoginAttemptService implements LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Override
    public void checkAllowed(String loginId, String remoteAddress) {
        String key = key(loginId, remoteAddress);
        Instant now = clock.instant();
        AttemptState state = attempts.computeIfPresent(key, (ignored, current) -> current.activeAt(now));
        if (isLoginBlocked(state, now)) {
            throw new TooManyLoginAttemptsException(Duration.between(now, state.blockedUntil()).toSeconds());
        }
    }

    @Override
    public void recordFailure(String loginId, String remoteAddress) {
        String key = key(loginId, remoteAddress);
        Instant now = clock.instant();
        attempts.compute(key, (ignored, current) -> {
            AttemptState state = current == null ? AttemptState.empty() : current.activeAt(now);
            return state.addFailure(now);
        });
    }

    @Override
    public void recordSuccess(String loginId, String remoteAddress) {
        attempts.remove(key(loginId, remoteAddress));
    }

    private boolean isLoginBlocked(AttemptState state, Instant now) {
        return state != null && state.isBlockedAt(now);
    }

    private String key(String loginId, String remoteAddress) {
        String normalizedLoginId = AuthAccount.normalizeLoginId(loginId);
        String source = normalizedLoginId + '|' + (remoteAddress == null ? "unknown" : remoteAddress);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private record AttemptState(List<Instant> failures, Instant blockedUntil) {

        private static AttemptState empty() {
            return new AttemptState(List.of(), null);
        }

        private AttemptState activeAt(Instant now) {
            if (hasBlockExpiredAt(now)) {
                return null;
            }
            Instant threshold = now.minus(FAILURE_WINDOW);
            List<Instant> activeFailures = failures.stream()
                    .filter(failure -> !failure.isBefore(threshold))
                    .toList();
            return new AttemptState(activeFailures, blockedUntil);
        }

        private AttemptState addFailure(Instant now) {
            List<Instant> nextFailures = new ArrayList<>(failures);
            nextFailures.add(now);
            Instant nextBlockedUntil = nextFailures.size() >= MAX_FAILURES
                    ? now.plus(BLOCK_DURATION)
                    : blockedUntil;
            return new AttemptState(List.copyOf(nextFailures), nextBlockedUntil);
        }

        private boolean hasBlockExpiredAt(Instant now) {
            return blockedUntil != null && !now.isBefore(blockedUntil);
        }

        private boolean isBlockedAt(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }
    }
}
