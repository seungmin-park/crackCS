package com.example.crackcs.content.knowledge.controller.request;

import com.example.crackcs.common.web.PageRequestFactory;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public record KnowledgeDocumentSearchRequest(
        @Positive(message = "topicId는 양수여야 합니다.")
        Long topicId,
        @Pattern(
                regexp = "DRAFT|PUBLISHED|RETIRED",
                message = "status는 DRAFT, PUBLISHED, RETIRED 중 하나여야 합니다."
        )
        String status,
        @Size(max = 100, message = "technologyVersion은 100자 이하여야 합니다.")
        String technologyVersion,
        @Min(value = 0, message = "page는 0 이상이어야 합니다.")
        Integer page,
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,
        List<String> sort
) {
    private static final Set<String> SORTABLE_PROPERTIES = Set.of(
            "id", "title", "documentVersion", "technologyVersion", "status", "createdAt", "updatedAt"
    );

    public KnowledgeDocumentStatus statusValue() {
        return status == null ? null : KnowledgeDocumentStatus.valueOf(status);
    }

    public Pageable toPageable() {
        return PageRequestFactory.create(page, size, sort, SORTABLE_PROPERTIES);
    }
}
