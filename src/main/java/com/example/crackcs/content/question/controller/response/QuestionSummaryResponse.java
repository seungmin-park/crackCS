package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;

import java.time.LocalDateTime;

public record QuestionSummaryResponse(
        Long id,
        Long topicId,
        QuestionOrigin origin,
        QuestionDifficulty difficulty,
        String content,
        QuestionStatus status,
        int questionVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuestionSummaryResponse from(Question question) {
        return new QuestionSummaryResponse(
                question.getId(),
                question.getTopicId(),
                question.getOrigin(),
                question.getDifficulty(),
                question.getContent(),
                question.getStatus(),
                question.getQuestionVersion(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}
