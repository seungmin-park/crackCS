package com.example.crackcs.learning.mastery.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.learning.mastery.domain.KnowledgeState;
import com.example.crackcs.learning.mastery.domain.TopicKnowledgeSummary;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult.ConceptState;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult.TopicState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultKnowledgeQueryService implements KnowledgeQueryService {
    private final KnowledgeStateRepository states;
    private final ConceptRepository concepts;
    private final TopicRepository topics;

    @Override
    public KnowledgeStatesResult knowledgeStates(Long memberId) {
        Map<Long, KnowledgeState> memberStates = states.findByMemberId(memberId).stream()
                .collect(Collectors.toMap(state -> state.getConcept().getId(), Function.identity()));
        Map<Long, List<Concept>> byTopic = concepts.findActiveWithActiveTopic().stream()
                .collect(Collectors.groupingBy(concept -> concept.getTopic().getId()));
        return new KnowledgeStatesResult(topics.findActiveOrderedById().stream()
                .map(topic -> response(topic, TopicKnowledgeSummary.from(
                        byTopic.getOrDefault(topic.getId(), List.of()), memberStates))).toList());
    }

    private TopicState response(Topic topic, TopicKnowledgeSummary knowledge) {
        List<ConceptState> results = knowledge.concepts().stream().map(concept -> new ConceptState(
                concept.conceptId(), concept.conceptName(), concept.status(), concept.masteryScore(),
                concept.confidenceScore(), concept.attemptCount(), concept.lastEvaluatedAt())).toList();
        return new TopicState(topic.getId(), topic.getName(), knowledge.status(), knowledge.masteryScore(),
                knowledge.confidenceScore(), knowledge.unknownCount(), knowledge.learningCount(),
                knowledge.stableCount(), results);
    }
}
