package com.example.crackcs.learning.mastery.service.result;

import com.example.crackcs.learning.mastery.domain.KnowledgeStatus;
import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeStatesResult(List<TopicState> topics) {
    public record TopicState(Long topicId, String topicName, KnowledgeStatus status, Double masteryScore,
                             double confidenceScore, long unknownCount, long learningCount, long stableCount,
                             List<ConceptState> concepts) {
    }

    public record ConceptState(Long conceptId, String conceptName, KnowledgeStatus status, Double masteryScore,
                               int confidenceScore, long attemptCount, LocalDateTime lastEvaluatedAt) {
    }
}
