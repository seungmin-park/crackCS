package com.example.crackcs.content.topic.controller.response;

import com.example.crackcs.content.topic.domain.Topic;

public record TopicResponse(
        Long id,
        Long parentId,
        String code,
        String name,
        boolean active
) {
    public static TopicResponse from(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getParent() == null ? null : topic.getParent().getId(),
                topic.getCode(),
                topic.getName(),
                topic.isActive()
        );
    }
}
