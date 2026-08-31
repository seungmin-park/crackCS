package com.example.crackcs.member.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.member.controller.response.MemberResponse;
import com.example.crackcs.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal AuthenticatedMember principal) {
        return MemberResponse.from(memberService.findById(principal.memberId()));
    }
}
