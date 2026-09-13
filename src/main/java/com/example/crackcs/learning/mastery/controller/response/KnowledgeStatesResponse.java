package com.example.crackcs.learning.mastery.controller.response;

import com.example.crackcs.learning.mastery.domain.KnowledgeStatus;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult;
import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeStatesResponse(List<TopicState> topics) {
    public static KnowledgeStatesResponse from(KnowledgeStatesResult result) {
        return new KnowledgeStatesResponse(result.topics().stream().map(TopicState::from).toList());
    }

    public record TopicState(Long topicId, String topicName, KnowledgeStatus status, Double masteryScore,
                             double confidenceScore, long unknownCount, long learningCount, long stableCount,
                             List<ConceptState> concepts) {
        public static TopicState from(KnowledgeStatesResult.TopicState result) {
            return new TopicState(result.topicId(), result.topicName(), result.status(), result.masteryScore(),
                    result.confidenceScore(), result.unknownCount(), result.learningCount(), result.stableCount(),
                    result.concepts().stream().map(ConceptState::from).toList());
        }
    }

    public record ConceptState(Long conceptId, String conceptName, KnowledgeStatus status, Double masteryScore,
                               int confidenceScore, long attemptCount, LocalDateTime lastEvaluatedAt) {
        public static ConceptState from(KnowledgeStatesResult.ConceptState result) {
            return new ConceptState(result.conceptId(), result.conceptName(), result.status(), result.masteryScore(),
                    result.confidenceScore(), result.attemptCount(), result.lastEvaluatedAt());
        }
    }
}
