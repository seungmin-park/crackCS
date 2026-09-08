package com.example.crackcs.learning.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerSubmitRequest(
        @NotBlank(message = "답변은 공백일 수 없습니다.")
        @Size(max = 10000, message = "답변은 10,000자 이하여야 합니다.") String content) {
    public String validatedRequestId(String header) {
        if (header == null || !header.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("Idempotency-Key는 소문자 UUID 형식이어야 합니다.");
        }
        return header;
    }
}
