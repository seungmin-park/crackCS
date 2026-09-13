package com.example.crackcs.learning.answer.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AnswerQuestionIdRequest(
        @NotNull(message = "questionId는 필수입니다.")
        @Positive(message = "questionId는 양수여야 합니다.")
        Long questionId
) {
}
