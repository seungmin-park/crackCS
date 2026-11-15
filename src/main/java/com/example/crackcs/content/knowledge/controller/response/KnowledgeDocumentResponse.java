package com.example.crackcs.content.knowledge.controller.response;

import com.example.crackcs.common.web.response.ApiTimestamp;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;

import java.time.Instant;

public record KnowledgeDocumentResponse(
        Long id,
        Long topicId,
        Long createdByMemberId,
        Long reviewedByMemberId,
        String title,
        KnowledgeSourceType sourceType,
        String sourceUrl,
        String versionSeriesId,
        int documentVersion,
        String technologyVersion,
        String licenseNote,
        String content,
        String checksum,
        KnowledgeDocumentStatus status,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getTopicId(),
                document.getCreatedByMemberId(),
                document.getReviewedByMemberId(),
                document.getTitle(),
                document.getSourceType(),
                document.getSourceUrl(),
                document.getVersionSeriesId(),
                document.getDocumentVersion(),
                document.getTechnologyVersion(),
                document.getLicenseNote(),
                document.getContent(),
                document.getChecksum(),
                document.getStatus(),
                ApiTimestamp.from(document.getReviewedAt()),
                ApiTimestamp.from(document.getCreatedAt()),
                ApiTimestamp.from(document.getUpdatedAt())
        );
    }
}
