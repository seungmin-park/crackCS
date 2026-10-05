package com.example.crackcs.auth.domain;

import com.example.crackcs.exception.TooManyAuthenticationRequestsException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "authentication_request_bucket", uniqueConstraints =
        @UniqueConstraint(name = "uk_auth_request_bucket_key", columnNames = "bucket_key"))
public class AuthenticationRequestBucket {
    public static final Duration WINDOW = Duration.ofMinutes(1);
    public static final String CAPACITY_KEY = key("capacity", "global");
    public static final String LOGIN_GLOBAL_KEY = key("login", "global");
    public static final String SIGN_UP_GLOBAL_KEY = key("sign-up", "global");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "bucket_key", nullable = false, length = 64, updatable = false)
    private String bucketKey;
    @Column(nullable = false)
    private Instant windowStartedAt;
    @Column(nullable = false)
    private int requestCount;

    @Builder
    private AuthenticationRequestBucket(String bucketKey, Instant now) {
        if (bucketKey == null || !bucketKey.matches("[0-9a-f]{64}") || now == null) {
            throw new IllegalArgumentException("bucket key and time are required");
        }
        this.bucketKey = bucketKey;
        this.windowStartedAt = now;
    }

    public void reserve(Instant now, int limit) {
        if (now == null || limit < 1) throw new IllegalArgumentException("time and positive limit are required");
        boolean expired = !now.isBefore(windowStartedAt.plus(WINDOW));
        int nextCount = expired ? 1 : Math.incrementExact(requestCount);
        if (nextCount > limit) {
            throw new TooManyAuthenticationRequestsException(Duration.between(now, windowStartedAt.plus(WINDOW)).toSeconds());
        }
        if (expired) windowStartedAt = now;
        requestCount = nextCount;
    }

    public static String sourceKey(String operation, String remoteAddress) {
        return key(operation, "source|" + (remoteAddress == null ? "unknown" : remoteAddress));
    }

    private static String key(String operation, String scope) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((operation + '|' + scope).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
