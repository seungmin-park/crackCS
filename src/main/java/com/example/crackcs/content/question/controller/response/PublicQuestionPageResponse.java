package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.content.question.domain.Question;
import org.springframework.data.domain.Page;

import java.util.List;

public record PublicQuestionPageResponse(
        List<PublicQuestionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PublicQuestionPageResponse from(Page<Question> questions) {
        return new PublicQuestionPageResponse(
                questions.getContent().stream()
                        .map(PublicQuestionResponse::from)
                        .toList(),
                questions.getNumber(),
                questions.getSize(),
                questions.getTotalElements(),
                questions.getTotalPages()
        );
    }
}
