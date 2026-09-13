package com.example.crackcs.learning.recommendation.service.result;

public record RecommendationResult(Long questionId, String title, Long conceptId, String conceptName,
                                   Reason reason, String reasonText) {
    public enum Reason {UNASSESSED_CONCEPT, LOW_MASTERY, NO_AVAILABLE_QUESTION}
}
