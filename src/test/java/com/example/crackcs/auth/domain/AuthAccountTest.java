package com.example.crackcs.auth.domain;

import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthAccountTest {

    @Test
    @DisplayName("LOCAL 인증 계정은 이메일을 정규화하고 해시만 보관한다")
    void createsLocalAccountWithNormalizedLoginId() {
        Member localMember = Member.builder().nickname("크랙러").build();

        AuthAccount localAccount = AuthAccount.builder()
                .member(localMember)
                .loginId("  USER@Example.COM ")
                .passwordHash("{bcrypt}encoded-password")
                .build();

        assertThat(localAccount.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(localAccount.getLoginId()).isEqualTo("user@example.com");
        assertThat(localAccount.getPasswordHash()).isEqualTo("{bcrypt}encoded-password");
        assertThat(localAccount.getCreatedAt()).isNotNull();
        assertThat(localAccount.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("활성 회원의 로그인 성공 시각을 기록한다")
    void recordsSuccessfulLogin() {
        AuthAccount activeAccount = AuthAccount.builder()
                .member(Member.builder().nickname("크랙러").build())
                .loginId("user@example.com")
                .passwordHash("{bcrypt}encoded-password")
                .build();

        activeAccount.recordSuccessfulLogin();

        assertThat(activeAccount.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("비활성 회원의 로그인 성공 시각은 기록할 수 없다")
    void rejectsLoginRecordForInactiveMember() {
        Member blockedMember = Member.builder().nickname("차단 회원").build();
        blockedMember.changeStatus(MemberStatus.BLOCKED);
        AuthAccount blockedAccount = AuthAccount.builder()
                .member(blockedMember)
                .loginId("blocked@example.com")
                .passwordHash("{bcrypt}encoded-password")
                .build();

        assertThatThrownBy(blockedAccount::recordSuccessfulLogin)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("inactive member must not authenticate");
        assertThat(blockedAccount.getLastLoginAt()).isNull();
    }
}
