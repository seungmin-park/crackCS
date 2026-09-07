package com.example.crackcs.learning.controller.request;
import jakarta.validation.constraints.*;
public record AnswerIdRequest(@NotNull @Positive(message = "답변 ID는 양수여야 합니다.") Long answerId) {}
