package com.example.crackcs.learning.mastery.domain;

import com.example.crackcs.content.concept.domain.Concept;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public record TopicKnowledgeSummary(List<ConceptKnowledge> concepts) {
    public TopicKnowledgeSummary {
        concepts = List.copyOf(concepts);
    }

    public static TopicKnowledgeSummary from(List<Concept> concepts, Map<Long, KnowledgeState> states) {
        return new TopicKnowledgeSummary(concepts.stream()
                .map(concept -> ConceptKnowledge.from(concept, states.get(concept.getId()))).toList());
    }

    public long unknownCount() {
        return concepts.stream().filter(concept -> concept.status() == KnowledgeStatus.UNKNOWN).count();
    }

    public long stableCount() {
        return concepts.stream().filter(concept -> concept.status() == KnowledgeStatus.STABLE).count();
    }

    public long learningCount() {
        return concepts.size() - unknownCount() - stableCount();
    }

    public KnowledgeStatus status() {
        if (concepts.isEmpty() || unknownCount() == concepts.size()) {
            return KnowledgeStatus.UNKNOWN;
        }
        return stableCount() == concepts.size() ? KnowledgeStatus.STABLE : KnowledgeStatus.LEARNING;
    }

    public Double masteryScore() {
        OptionalDouble average = concepts.stream().filter(concept -> concept.masteryScore() != null)
                .mapToDouble(ConceptKnowledge::masteryScore).average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

    public double confidenceScore() {
        return concepts.stream().mapToInt(ConceptKnowledge::confidenceScore).average().orElse(0);
    }

    public record ConceptKnowledge(Long conceptId, String conceptName, KnowledgeStatus status,
                                   Double masteryScore, int confidenceScore, long attemptCount,
                                   LocalDateTime lastEvaluatedAt) {
        private static ConceptKnowledge from(Concept concept, KnowledgeState state) {
            return state == null
                    ? new ConceptKnowledge(concept.getId(), concept.getName(), KnowledgeStatus.UNKNOWN, null, 0, 0,
                    null)
                    : new ConceptKnowledge(concept.getId(), concept.getName(), state.getStatus(),
                    state.getMasteryScore(), state.getConfidenceScore(), state.getAttemptCount(),
                    state.getLastEvaluatedAt());
        }
    }
}
