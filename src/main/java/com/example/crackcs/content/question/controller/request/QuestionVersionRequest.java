package com.example.crackcs.content.question.controller.request;

import com.example.crackcs.content.question.domain.QuestionDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionVersionRequest(
        @NotNull(message = "difficulty는 필수입니다.")
        QuestionDifficulty difficulty,
        @NotBlank(message = "content는 공백일 수 없습니다.")
        String content,
        @NotBlank(message = "referenceAnswer는 공백일 수 없습니다.")
        String referenceAnswer
) {
}
