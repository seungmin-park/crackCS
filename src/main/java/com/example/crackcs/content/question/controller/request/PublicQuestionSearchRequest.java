package com.example.crackcs.content.question.controller.request;

import com.example.crackcs.common.web.PageRequestFactory;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;

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
        return PageRequestFactory.create(page, size, sort, SORTABLE_PROPERTIES);
    }
}
