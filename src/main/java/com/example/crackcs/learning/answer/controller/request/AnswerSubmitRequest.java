package com.example.crackcs.learning.answer.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerSubmitRequest(
        @NotBlank(message = "답변은 공백일 수 없습니다.")
        @Size(max = 10000, message = "답변은 10,000자 이하여야 합니다.") String content) {
}
