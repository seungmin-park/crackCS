package com.example.crackcs.learning.progress.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.learning.progress.controller.response.ProgressResponse;
import com.example.crackcs.learning.progress.service.LearningProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LearningProgressController {
    private final LearningProgressService learningProgressService;

    @GetMapping("/api/members/me/progress")
    public ProgressResponse progress(@AuthenticationPrincipal AuthenticatedMember member) {
        return ProgressResponse.from(learningProgressService.progress(member.memberId()));
    }
}
