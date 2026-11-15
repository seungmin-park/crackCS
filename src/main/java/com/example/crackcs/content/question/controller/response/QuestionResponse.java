package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.common.web.response.ApiTimestamp;
import com.example.crackcs.content.question.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuestionResponse(
        Long id,
        Long topicId,
        QuestionOrigin origin,
        QuestionType type,
        QuestionDifficulty difficulty,
        String content,
        String referenceAnswer,
        QuestionStatus status,
        String versionSeriesId,
        int questionVersion,
        Long createdByMemberId,
        Long reviewedByMemberId,
        Instant reviewedAt,
        List<QuestionConceptResponse> concepts,
        Instant createdAt,
        Instant updatedAt
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
                question.getVersionSeriesId(),
                question.getQuestionVersion(),
                question.getCreatedByMemberId(),
                question.getReviewedByMemberId(),
                ApiTimestamp.from(question.getReviewedAt()),
                question.getQuestionConcepts().stream()
                        .map(concept -> new QuestionConceptResponse(
                                concept.getConcept().getId(),
                                concept.getConcept().getCode(),
                                concept.getConcept().getName(),
                                concept.getWeight(),
                                concept.isRequired()
                        ))
                        .toList(),
                ApiTimestamp.from(question.getCreatedAt()),
                ApiTimestamp.from(question.getUpdatedAt())
        );
    }

    public record QuestionConceptResponse(
            Long conceptId,
            String code,
            String name,
            BigDecimal weight,
            boolean required
    ) {
    }
}
