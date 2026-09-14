package com.example.crackcs.learning.mastery.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.learning.mastery.controller.response.KnowledgeStatesResponse;
import com.example.crackcs.learning.mastery.service.KnowledgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeQueryService knowledgeQueryService;

    @GetMapping("/api/members/me/knowledge-states")
    public KnowledgeStatesResponse knowledgeStates(@AuthenticationPrincipal AuthenticatedMember member) {
        return KnowledgeStatesResponse.from(knowledgeQueryService.knowledgeStates(member.memberId()));
    }
}
