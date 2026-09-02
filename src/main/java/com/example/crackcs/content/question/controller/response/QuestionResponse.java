package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.domain.QuestionType;

import java.time.LocalDateTime;
import java.math.BigDecimal;
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
        LocalDateTime reviewedAt,
        List<QuestionConceptResponse> concepts,
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
                question.getVersionSeriesId(),
                question.getQuestionVersion(),
                question.getCreatedByMemberId(),
                question.getReviewedByMemberId(),
                question.getReviewedAt(),
                question.getQuestionConcepts().stream()
                        .map(concept -> new QuestionConceptResponse(
                                concept.getConcept().getId(),
                                concept.getConcept().getCode(),
                                concept.getConcept().getName(),
                                concept.getWeight(),
                                concept.isRequired()
                        ))
                        .toList(),
                question.getCreatedAt(),
                question.getUpdatedAt()
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
