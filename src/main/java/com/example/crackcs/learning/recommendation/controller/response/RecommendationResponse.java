package com.example.crackcs.learning.recommendation.controller.response;

import com.example.crackcs.learning.recommendation.service.result.RecommendationResult;

public record RecommendationResponse(Long questionId, String title, Long conceptId, String conceptName,
                                     Reason reason, String reasonText) {
    public static RecommendationResponse from(RecommendationResult result) {
        return new RecommendationResponse(result.questionId(), result.title(), result.conceptId(), result.conceptName(),
                switch (result.reason()) {
                    case UNASSESSED_CONCEPT -> Reason.UNASSESSED_CONCEPT;
                    case LOW_MASTERY -> Reason.LOW_MASTERY;
                    case NO_AVAILABLE_QUESTION -> Reason.NO_AVAILABLE_QUESTION;
                }, result.reasonText());
    }

    public enum Reason {UNASSESSED_CONCEPT, LOW_MASTERY, NO_AVAILABLE_QUESTION}
}
