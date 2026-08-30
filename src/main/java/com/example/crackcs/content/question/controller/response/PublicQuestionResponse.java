package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.topic.domain.Topic;

public record PublicQuestionResponse(
        Long id,
        TopicResponse topic,
        QuestionDifficulty difficulty,
        String content
) {

    public static PublicQuestionResponse from(Question question) {
        return new PublicQuestionResponse(
                question.getId(),
                TopicResponse.from(question.getTopic()),
                question.getDifficulty(),
                question.getContent()
        );
    }

    public record TopicResponse(Long id, String code, String name) {

        private static TopicResponse from(Topic topic) {
            return new TopicResponse(topic.getId(), topic.getCode(), topic.getName());
        }
    }
}
