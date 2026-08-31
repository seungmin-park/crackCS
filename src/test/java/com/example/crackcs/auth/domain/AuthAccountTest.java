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
        Member member = Member.builder().nickname("크랙러").build();

        AuthAccount account = AuthAccount.builder()
                .member(member)
                .loginId("  USER@Example.COM ")
                .passwordHash("{bcrypt}encoded-password")
                .build();

        assertThat(account.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(account.getLoginId()).isEqualTo("user@example.com");
        assertThat(account.getPasswordHash()).isEqualTo("{bcrypt}encoded-password");
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("활성 회원의 로그인 성공 시각을 기록한다")
    void recordsSuccessfulLogin() {
        AuthAccount account = AuthAccount.builder()
                .member(Member.builder().nickname("크랙러").build())
                .loginId("user@example.com")
                .passwordHash("{bcrypt}encoded-password")
                .build();

        account.recordSuccessfulLogin();

        assertThat(account.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("비활성 회원의 로그인 성공 시각은 기록할 수 없다")
    void rejectsLoginRecordForInactiveMember() {
        Member member = Member.builder().nickname("차단 회원").build();
        member.changeStatus(MemberStatus.BLOCKED);
        AuthAccount account = AuthAccount.builder()
                .member(member)
                .loginId("blocked@example.com")
                .passwordHash("{bcrypt}encoded-password")
                .build();

        assertThatThrownBy(account::recordSuccessfulLogin)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("inactive member must not authenticate");
        assertThat(account.getLastLoginAt()).isNull();
    }
}
