package com.example.crackcs.learning.progress.service.result;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult;
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult;
import java.time.LocalDateTime;
import java.util.List;

public record LearningProgressResult(long totalAnswers, long recentAnswerCount,
                                     List<RecentEvaluation> recentEvaluations,
                                     List<KnowledgeStatesResult.TopicState> topics,
                                     RecommendationResult recommendation) {
    public record RecentEvaluation(Long answerId, String questionTitle, EvaluationStatus status, Verdict verdict,
                                   Integer score, LocalDateTime submittedAt) {
    }
}
