package com.example.crackcs.learning.followup.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.learning.followup.controller.request.FollowUpAnswerIdRequest;
import com.example.crackcs.learning.followup.controller.response.FollowUpQuestionResponse;
import com.example.crackcs.learning.followup.service.FollowUpQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FollowUpQuestionController {
    private final FollowUpQuestionService service;

    @GetMapping("/api/answers/{answerId}/follow-up-question")
    public FollowUpQuestionResponse find(@AuthenticationPrincipal AuthenticatedMember member,
                                         @Valid @ModelAttribute FollowUpAnswerIdRequest path) {
        return FollowUpQuestionResponse.from(service.findByAnswerId(member.memberId(), path.answerId()));
    }
}
