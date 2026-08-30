package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.domain.QuestionType;

import java.time.LocalDateTime;

public record QuestionResponse(
        Long id,
        Long topicId,
        QuestionOrigin origin,
        QuestionType type,
        QuestionDifficulty difficulty,
        String content,
        String referenceAnswer,
        QuestionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getTopicId(),
                question.getOrigin(),
                question.getType(),
                question.getDifficulty(),
                question.getContent(),
                question.getReferenceAnswer(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}
