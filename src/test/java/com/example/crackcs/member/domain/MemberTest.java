package com.example.crackcs.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    @DisplayName("회원을 생성하면 활성 USER와 날짜 정보를 초기화한다")
    void createsActiveUserWithAuditDates() {
        LocalDateTime before = LocalDateTime.now();

        Member member = Member.builder()
                .nickname("크랙러")
                .build();

        LocalDateTime after = LocalDateTime.now();
        assertThat(member.getNickname()).isEqualTo("크랙러");
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getCreatedAt()).isBetween(before, after);
        assertThat(member.getUpdatedAt()).isEqualTo(member.getCreatedAt());
        assertThat(member.isAuthenticatable()).isTrue();
    }

    @Test
    @DisplayName("차단되거나 탈퇴한 회원은 인증할 수 없다")
    void rejectsAuthenticationForInactiveMember() {
        Member blocked = Member.builder().nickname("차단 회원").build();
        Member withdrawn = Member.builder().nickname("탈퇴 회원").build();

        blocked.changeStatus(MemberStatus.BLOCKED);
        withdrawn.changeStatus(MemberStatus.WITHDRAWN);

        assertThat(blocked.isAuthenticatable()).isFalse();
        assertThat(withdrawn.isAuthenticatable()).isFalse();
    }

    @Test
    @DisplayName("닉네임을 수정하면 생성 시각은 유지하고 수정 시각을 갱신한다")
    void updatesNicknameAndModifiedDate() {
        Member member = Member.builder().nickname("이전 닉네임").build();
        LocalDateTime createdAt = member.getCreatedAt();

        member.updateNickname("새 닉네임");

        assertThat(member.getNickname()).isEqualTo("새 닉네임");
        assertThat(member.getCreatedAt()).isEqualTo(createdAt);
        assertThat(member.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }
}
