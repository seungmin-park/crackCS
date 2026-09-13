package com.example.crackcs.learning.recommendation.controller;

import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.learning.recommendation.controller.response.RecommendationResponse;
import com.example.crackcs.learning.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService service;

    @GetMapping("/api/recommendations/next-question")
    public RecommendationResponse recommendation(@AuthenticationPrincipal AuthenticatedMember member) {
        return RecommendationResponse.from(service.recommendation(member.memberId()));
    }
}
