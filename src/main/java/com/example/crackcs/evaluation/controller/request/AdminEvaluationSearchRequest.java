package com.example.crackcs.evaluation.controller.request;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record AdminEvaluationSearchRequest(
        @Pattern(regexp = "FAILED|NEEDS_REVIEW", message = "status는 FAILED 또는 NEEDS_REVIEW여야 합니다.")
        String status,
        @Min(value = 0, message = "page는 0 이상이어야 합니다.") Integer page,
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.") Integer size
) {
    public EvaluationStatus statusValue() {
        return status == null ? null : EvaluationStatus.valueOf(status);
    }

    public Pageable pageable() {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Order.desc("evaluatedAt"), Sort.Order.desc("id")));
    }
}
