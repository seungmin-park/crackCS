package com.example.crackcs.content.topic.service;

import com.example.crackcs.content.topic.domain.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TopicService {

    Topic create(Long parentId, String code, String name);

    Page<Topic> findAll(Long parentId, Boolean active, Pageable pageable);

    Topic findById(Long topicId);

    Topic update(Long topicId, Long parentId, String code, String name);

    void deactivate(Long topicId);
}
