package com.example.crackcs.learning.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AnswerIdRequest(@NotNull @Positive(message = "답변 ID는 양수여야 합니다.") Long answerId) {
}
