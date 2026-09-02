package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.content.question.domain.Question;
import org.springframework.data.domain.Page;

import java.util.List;

public record QuestionPageResponse(
        List<QuestionSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static QuestionPageResponse from(Page<Question> questions) {
        return new QuestionPageResponse(
                questions.getContent().stream()
                        .map(QuestionSummaryResponse::from)
                        .toList(),
                questions.getNumber(),
                questions.getSize(),
                questions.getTotalElements(),
                questions.getTotalPages()
        );
    }
}
