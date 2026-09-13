package com.example.crackcs.learning.recommendation.service;

import com.example.crackcs.learning.recommendation.service.result.RecommendationResult;

public interface RecommendationService {
    RecommendationResult recommendation(Long memberId);
}
