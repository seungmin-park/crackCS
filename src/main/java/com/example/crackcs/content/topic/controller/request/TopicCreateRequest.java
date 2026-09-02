package com.example.crackcs.content.topic.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TopicCreateRequest(
        @Positive(message = "parentId는 양수여야 합니다.")
        Long parentId,

        @NotBlank(message = "code는 공백일 수 없습니다.")
        @Size(max = 50, message = "code는 50자 이하여야 합니다.")
        String code,

        @NotBlank(message = "name은 공백일 수 없습니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name
) {
}
