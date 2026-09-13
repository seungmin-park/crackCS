package com.example.crackcs.learning.progress.controller.response;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.learning.mastery.controller.response.KnowledgeStatesResponse;
import com.example.crackcs.learning.progress.service.result.LearningProgressResult;
import com.example.crackcs.learning.recommendation.controller.response.RecommendationResponse;
import java.time.LocalDateTime;
import java.util.List;

public record ProgressResponse(long totalAnswers, long recentAnswerCount, List<RecentEvaluation> recentEvaluations,
                               List<KnowledgeStatesResponse.TopicState> topics, RecommendationResponse recommendation) {
    public static ProgressResponse from(LearningProgressResult result) {
        return new ProgressResponse(result.totalAnswers(), result.recentAnswerCount(),
                result.recentEvaluations().stream().map(RecentEvaluation::from).toList(),
                result.topics().stream().map(KnowledgeStatesResponse.TopicState::from).toList(),
                RecommendationResponse.from(result.recommendation()));
    }

    public record RecentEvaluation(Long answerId, String questionTitle, EvaluationStatus status, Verdict verdict,
                                   Integer score, LocalDateTime submittedAt) {
        public static RecentEvaluation from(LearningProgressResult.RecentEvaluation result) {
            return new RecentEvaluation(result.answerId(), result.questionTitle(), result.status(), result.verdict(),
                    result.score(), result.submittedAt());
        }
    }
}
