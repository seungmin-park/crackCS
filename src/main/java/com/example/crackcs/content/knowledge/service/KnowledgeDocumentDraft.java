package com.example.crackcs.content.knowledge.service;

import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;

public record KnowledgeDocumentDraft(
        Long topicId,
        String title,
        KnowledgeSourceType sourceType,
        String sourceUrl,
        String technologyVersion,
        String licenseNote,
        String content
) {
}
