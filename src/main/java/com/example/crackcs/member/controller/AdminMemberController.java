package com.example.crackcs.member.controller;

import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.member.controller.request.AdminMemberSearchRequest;
import com.example.crackcs.member.controller.request.MemberStatusUpdateRequest;
import com.example.crackcs.member.controller.response.MemberResponse;
import com.example.crackcs.member.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping
    public PageResponse<MemberResponse> findAll(@Valid @ModelAttribute AdminMemberSearchRequest request) {
        return PageResponse.from(
                memberService.findAll(request.role(), request.status(), request.toPageable()),
                MemberResponse::from
        );
    }

    @PatchMapping("/{memberId}/status")
    public MemberResponse changeStatus(
            @Positive(message = "memberId는 양수여야 합니다.") @PathVariable Long memberId,
            @Valid @RequestBody MemberStatusUpdateRequest request
    ) {
        return MemberResponse.from(memberService.changeStatus(memberId, request.status()));
    }
}
