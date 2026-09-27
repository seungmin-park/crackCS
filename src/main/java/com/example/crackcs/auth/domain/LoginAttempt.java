package com.example.crackcs.auth.domain;

import com.example.crackcs.exception.TooManyLoginAttemptsException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "login_attempt", uniqueConstraints = @UniqueConstraint(name = "uk_login_attempt_key", columnNames = "attempt_key"))
public class LoginAttempt {
    public static final Duration FAILURE_WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    private static final int MAX_FAILURES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_key", nullable = false, length = 64, updatable = false)
    private String attemptKey;

    @ElementCollection
    @CollectionTable(name = "login_attempt_failure", joinColumns = @JoinColumn(name = "login_attempt_id"))
    @OrderColumn(name = "sequence_no")
    @Column(name = "failed_at", nullable = false)
    @Getter(AccessLevel.NONE)
    private List<Instant> failures = new ArrayList<>();

    private Instant blockedUntil;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Builder
    private LoginAttempt(String attemptKey, Instant now) {
        if (attemptKey == null || !attemptKey.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("attemptKey must be a SHA-256 digest");
        }
        requireTime(now);
        this.attemptKey = attemptKey;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public List<Instant> getFailures() {
        return List.copyOf(failures);
    }

    public void ensureAllowed(Instant now) {
        requireTime(now);
        if (isBlockedAt(now)) {
            throw new TooManyLoginAttemptsException(Duration.between(now, blockedUntil).toSeconds());
        }
    }

    public void recordFailure(Instant now) {
        requireTime(now);
        if (isBlockedAt(now)) return;
        if (blockedUntil != null) {
            failures.clear();
            blockedUntil = null;
        }
        Instant threshold = now.minus(FAILURE_WINDOW);
        failures.removeIf(failure -> failure.isBefore(threshold));
        failures.add(now);
        if (failures.size() >= MAX_FAILURES) blockedUntil = now.plus(BLOCK_DURATION);
        updatedAt = now;
    }

    public boolean isExpiredAt(Instant now) {
        requireTime(now);
        return blockedUntil != null ? !now.isBefore(blockedUntil) : updatedAt.isBefore(now.minus(FAILURE_WINDOW));
    }

    private boolean isBlockedAt(Instant now) {
        return blockedUntil != null && now.isBefore(blockedUntil);
    }

    private static void requireTime(Instant now) {
        if (now == null) throw new IllegalArgumentException("now must not be null");
    }
}
