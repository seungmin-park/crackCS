package com.example.crackcs.content.question.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuestionIdRequest(
        @NotNull(message = "questionId는 필수입니다.")
        @Positive(message = "questionId는 양수여야 합니다.")
        Long questionId
) {
}
