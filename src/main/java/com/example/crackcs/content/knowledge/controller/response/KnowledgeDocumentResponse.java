package com.example.crackcs.content.knowledge.controller.response;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;

import java.time.LocalDateTime;

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
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
                document.getReviewedAt(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
