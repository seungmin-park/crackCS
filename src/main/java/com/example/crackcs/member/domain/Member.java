package com.example.crackcs.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Member(String nickname, MemberRole role) {
        this.nickname = requireNickname(nickname);
        this.role = role == null ? MemberRole.USER : role;
        this.status = MemberStatus.ACTIVE;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateNickname(String nickname) {
        this.nickname = requireNickname(nickname);
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(MemberStatus status) {
        this.status = requireNonNull(status, "status");
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAuthenticatable() {
        return status == MemberStatus.ACTIVE;
    }

    private static String requireNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname must not be blank");
        }
        String normalized = nickname.trim();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("nickname must be 100 characters or fewer");
        }
        return normalized;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
