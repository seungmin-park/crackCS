package com.example.crackcs.content.concept.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.exception.ConceptNotFoundException;
import com.example.crackcs.exception.DuplicateContentCodeException;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.exception.TopicNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultConceptService implements ConceptService {

    private final ConceptRepository conceptRepository;
    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public Concept create(Long topicId, String code, String name, String description) {
        Topic topic = findActiveTopic(topicId);
        ensureUniqueCode(code, null);
        return conceptRepository.save(Concept.builder()
                .topic(topic)
                .code(code)
                .name(name)
                .description(description)
                .build());
    }

    @Override
    public Page<Concept> findAll(Long topicId, Boolean active, Pageable pageable) {
        return conceptRepository.findAllByConditions(topicId, active, pageable);
    }

    @Override
    public Concept findById(Long conceptId) {
        return findConcept(conceptId);
    }

    @Override
    @Transactional
    public Concept update(Long conceptId, Long topicId, String code, String name, String description) {
        Concept concept = findConcept(conceptId);
        Topic topic = findActiveTopic(topicId);
        ensureUniqueCode(code, conceptId);
        concept.update(topic, code, name, description);
        return concept;
    }

    @Override
    @Transactional
    public void deactivate(Long conceptId) {
        findConcept(conceptId).deactivate();
    }

    private void ensureUniqueCode(String code, Long conceptId) {
        boolean duplicated = conceptId == null
                ? conceptRepository.existsByCode(code)
                : conceptRepository.existsByCodeAndIdNot(code, conceptId);
        if (duplicated) {
            throw new DuplicateContentCodeException("Concept", code);
        }
    }

    private Topic findActiveTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));
        if (!topic.isActive()) {
            throw new InvalidContentStateException("비활성 Topic에는 Concept을 연결할 수 없습니다.");
        }
        return topic;
    }

    private Concept findConcept(Long conceptId) {
        return conceptRepository.findById(conceptId)
                .orElseThrow(() -> new ConceptNotFoundException(conceptId));
    }
}
