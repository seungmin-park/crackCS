package com.example.crackcs.content.question.controller.request;

import com.example.crackcs.content.question.domain.QuestionDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record QuestionCreateRequest(
        @NotNull(message = "topicId는 필수입니다.")
        @Positive(message = "topicId는 양수여야 합니다.")
        Long topicId,

        @NotBlank(message = "difficulty는 필수입니다.")
        @Pattern(
                regexp = "BASIC|INTERMEDIATE|ADVANCED",
                message = "difficulty는 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다."
        )
        String difficulty,

        @NotBlank(message = "content는 공백일 수 없습니다.")
        String content,

        @NotBlank(message = "referenceAnswer는 공백일 수 없습니다.")
        String referenceAnswer
) {

    public QuestionDifficulty difficultyValue() {
        return QuestionDifficulty.valueOf(difficulty);
    }
}
