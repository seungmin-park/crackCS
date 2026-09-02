package com.example.crackcs.member.controller.request;

import com.example.crackcs.member.domain.MemberStatus;
import jakarta.validation.constraints.NotNull;

public record MemberStatusUpdateRequest(
        @NotNull(message = "status는 필수입니다.")
        MemberStatus status
) {
}
