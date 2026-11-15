package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.common.web.response.ApiTimestamp;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;

import java.time.Instant;

public record QuestionSummaryResponse(
        Long id,
        Long topicId,
        QuestionOrigin origin,
        QuestionDifficulty difficulty,
        String content,
        QuestionStatus status,
        int questionVersion,
        Instant createdAt,
        Instant updatedAt
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
                ApiTimestamp.from(question.getCreatedAt()),
                ApiTimestamp.from(question.getUpdatedAt())
        );
    }
}
