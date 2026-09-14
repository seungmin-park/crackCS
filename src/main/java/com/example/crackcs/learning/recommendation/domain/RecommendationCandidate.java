package com.example.crackcs.learning.recommendation.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.learning.mastery.domain.KnowledgeState;

import java.time.LocalDateTime;

public record RecommendationCandidate(Question question, Concept concept, Double mastery, LocalDateTime lastAnswered)
        implements Comparable<RecommendationCandidate> {
    public static RecommendationCandidate from(Question question, Concept concept, KnowledgeState state,
                                               LocalDateTime lastAnswered) {
        return new RecommendationCandidate(question, concept, state == null ? null : state.getMasteryScore(),
                lastAnswered);
    }

    public boolean isUnassessed() {
        return mastery == null;
    }

    private RecommendationPriority priority() {
        return new RecommendationPriority(mastery, lastAnswered, question.getId(), concept.getId());
    }

    @Override
    public int compareTo(RecommendationCandidate other) {
        return priority().compareTo(other.priority());
    }
}
