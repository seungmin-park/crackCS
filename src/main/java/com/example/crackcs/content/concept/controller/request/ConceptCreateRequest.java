package com.example.crackcs.content.concept.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConceptCreateRequest(
        @NotNull(message = "topicId는 필수입니다.")
        @Positive(message = "topicId는 양수여야 합니다.")
        Long topicId,
        @NotBlank(message = "code는 공백일 수 없습니다.")
        @Size(max = 100, message = "code는 100자 이하여야 합니다.")
        String code,
        @NotBlank(message = "name은 공백일 수 없습니다.")
        @Size(max = 150, message = "name은 150자 이하여야 합니다.")
        String name,
        String description
) {
}
