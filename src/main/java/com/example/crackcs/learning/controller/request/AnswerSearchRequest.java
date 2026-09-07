package com.example.crackcs.learning.controller.request;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
public record AnswerSearchRequest(
        @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") Integer page,
        @Min(value = 1, message = "크기는 1 이상이어야 합니다.")
        @Max(value = 100, message = "크기는 100 이하여야 합니다.") Integer size) {
    public Pageable toPageable() {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id")));
    }
}
