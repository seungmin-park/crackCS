package com.example.crackcs.content.topic.service;

import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
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
public class DefaultTopicService implements TopicService {

    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public Topic create(Long parentId, String code, String name) {
        ensureUniqueCode(code, null);
        Topic parent = findActiveParent(parentId);
        return topicRepository.save(Topic.builder()
                .parent(parent)
                .code(code)
                .name(name)
                .build());
    }

    @Override
    public Page<Topic> findAll(Long parentId, Boolean active, Pageable pageable) {
        return topicRepository.findAllByConditions(parentId, active, pageable);
    }

    @Override
    public Topic findById(Long topicId) {
        return findTopic(topicId);
    }

    @Override
    @Transactional
    public Topic update(Long topicId, Long parentId, String code, String name) {
        Topic topic = findTopic(topicId);
        Topic parent = findActiveParent(parentId);
        ensureUniqueCode(code, topicId);
        ensureAcyclic(topic, parent);
        topic.update(parent, code, name);
        return topic;
    }

    @Override
    @Transactional
    public void deactivate(Long topicId) {
        findTopic(topicId).deactivate();
    }

    private Topic findActiveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        Topic parent = findTopic(parentId);
        if (!parent.isActive()) {
            throw new InvalidContentStateException("비활성 Topic은 상위 Topic으로 지정할 수 없습니다.");
        }
        return parent;
    }

    private void ensureUniqueCode(String code, Long topicId) {
        boolean duplicated = topicId == null
                ? topicRepository.existsByCode(code)
                : topicRepository.existsByCodeAndIdNot(code, topicId);
        if (duplicated) {
            throw new DuplicateContentCodeException("Topic", code);
        }
    }

    private void ensureAcyclic(Topic topic, Topic candidateParent) {
        Topic current = candidateParent;
        while (current != null) {
            if (current == topic || sameId(current, topic)) {
                throw new InvalidContentStateException("Topic 계층에 순환을 만들 수 없습니다.");
            }
            current = current.getParent();
        }
    }

    private boolean sameId(Topic left, Topic right) {
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private Topic findTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));
    }
}
