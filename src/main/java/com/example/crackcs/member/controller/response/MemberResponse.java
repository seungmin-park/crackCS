package com.example.crackcs.member.controller.response;

import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;

public record MemberResponse(
        Long id,
        String nickname,
        MemberRole role,
        MemberStatus status
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getRole(),
                member.getStatus()
        );
    }
}
