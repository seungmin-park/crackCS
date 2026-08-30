package com.example.crackcs.content.question.controller.request;

import com.example.crackcs.content.question.domain.QuestionDifficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record PublicQuestionSearchRequest(
        @Positive(message = "topicId는 양수여야 합니다.")
        Long topicId,

        @Pattern(
                regexp = "BASIC|INTERMEDIATE|ADVANCED",
                message = "difficulty는 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다."
        )
        String difficulty,

        @Min(value = 0, message = "page는 0 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,

        List<String> sort
) {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("id", "difficulty", "createdAt");

    public QuestionDifficulty difficultyValue() {
        return difficulty == null ? null : QuestionDifficulty.valueOf(difficulty);
    }

    public Pageable toPageable() {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        return PageRequest.of(pageNumber, pageSize, toSort());
    }

    private Sort toSort() {
        if (sort == null || sort.isEmpty()) {
            return Sort.by(Sort.Order.asc("id"));
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (int index = 0; index < sort.size(); index++) {
            String property = sort.get(index);
            Sort.Direction direction = Sort.Direction.ASC;
            if (index + 1 < sort.size() && isDirection(sort.get(index + 1))) {
                direction = Sort.Direction.fromString(sort.get(++index));
            }
            if (!SORTABLE_PROPERTIES.contains(property)) {
                throw new IllegalArgumentException("지원하지 않는 정렬 필드입니다: " + property);
            }
            orders.add(new Sort.Order(direction, property));
        }
        return Sort.by(orders);
    }

    private boolean isDirection(String value) {
        return "asc".equalsIgnoreCase(value) || "desc".equalsIgnoreCase(value);
    }
}
