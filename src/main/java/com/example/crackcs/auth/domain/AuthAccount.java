package com.example.crackcs.auth.domain;

import com.example.crackcs.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "auth_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_auth_account_provider_login_id",
                columnNames = {"provider", "login_id"}
        )
)
public class AuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthProvider provider;

    @Column(name = "login_id", nullable = false, length = 255)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private AuthAccount(Member member, String loginId, String passwordHash) {
        this.member = requireNonNull(member, "member");
        this.provider = AuthProvider.LOCAL;
        this.loginId = normalizeLoginId(loginId);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.createdAt = LocalDateTime.now();
    }

    public void recordSuccessfulLogin() {
        if (!member.isAuthenticatable()) {
            throw new IllegalStateException("inactive member must not authenticate");
        }
        this.lastLoginAt = LocalDateTime.now();
    }

    public Long getMemberId() {
        return member.getId();
    }

    public static String normalizeLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId must not be blank");
        }
        String normalized = loginId.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("loginId must be 255 characters or fewer");
        }
        return normalized;
    }

    private static String requirePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        if (passwordHash.length() > 255) {
            throw new IllegalArgumentException("passwordHash must be 255 characters or fewer");
        }
        return passwordHash;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
