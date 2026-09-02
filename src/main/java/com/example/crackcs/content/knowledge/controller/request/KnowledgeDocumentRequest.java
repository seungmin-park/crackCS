package com.example.crackcs.content.knowledge.controller.request;

import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.service.KnowledgeDocumentData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentRequest(
        @NotNull(message = "topicId는 필수입니다.")
        @Positive(message = "topicId는 양수여야 합니다.")
        Long topicId,

        @NotBlank(message = "title은 공백일 수 없습니다.")
        @Size(max = 255, message = "title은 255자 이하여야 합니다.")
        String title,

        @NotNull(message = "sourceType은 필수입니다.")
        KnowledgeSourceType sourceType,

        @Size(max = 1000, message = "sourceUrl은 1000자 이하여야 합니다.")
        @Pattern(
                regexp = "^$|https?://.+",
                message = "sourceUrl은 HTTP 또는 HTTPS URL이어야 합니다."
        )
        String sourceUrl,

        @Size(max = 100, message = "technologyVersion은 100자 이하여야 합니다.")
        String technologyVersion,

        @Size(max = 500, message = "licenseNote는 500자 이하여야 합니다.")
        String licenseNote,

        @NotBlank(message = "content는 공백일 수 없습니다.")
        String content
) {
    public KnowledgeDocumentData toData() {
        return new KnowledgeDocumentData(
                topicId,
                title,
                sourceType,
                sourceUrl,
                technologyVersion,
                licenseNote,
                content
        );
    }
}
