package com.example.crackcs.content.question.controller.request;

import com.example.crackcs.content.question.service.QuestionConceptData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record QuestionConceptReplaceRequest(
        @NotNull(message = "concepts는 필수입니다.")
        List<@Valid Item> concepts
) {
    public List<QuestionConceptData> toData() {
        return concepts.stream()
                .map(item -> new QuestionConceptData(item.conceptId(), item.weight(), item.required()))
                .toList();
    }

    public record Item(
            @NotNull(message = "conceptId는 필수입니다.")
            @Positive(message = "conceptId는 양수여야 합니다.")
            Long conceptId,
            @NotNull(message = "weight는 필수입니다.")
            @DecimalMin(value = "0.00", inclusive = false, message = "weight는 0보다 커야 합니다.")
            @DecimalMax(value = "1.00", message = "weight는 1 이하여야 합니다.")
            @Digits(integer = 1, fraction = 2, message = "weight는 소수 둘째 자리까지 입력할 수 있습니다.")
            BigDecimal weight,
            boolean required
    ) {
    }
}
