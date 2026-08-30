package com.example.crackcs.exception;

public class TopicNotFoundException extends RuntimeException {

    public TopicNotFoundException(Long topicId) {
        super("Topic not found: " + topicId);
    }
}
