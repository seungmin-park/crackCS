package com.example.crackcs.content.knowledge.service;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeDocumentService {

    KnowledgeDocument create(Long creatorMemberId, KnowledgeDocumentDraft draft);

    Page<KnowledgeDocument> findAll(
            Long topicId,
            KnowledgeDocumentStatus status,
            String technologyVersion,
            Pageable pageable
    );

    KnowledgeDocument findById(Long documentId);

    KnowledgeDocument update(Long documentId, KnowledgeDocumentDraft draft);

    KnowledgeDocument createNextVersion(Long documentId, Long creatorMemberId, KnowledgeDocumentDraft draft);

    KnowledgeDocument review(Long documentId, Long reviewerMemberId);

    KnowledgeDocument publishAsCurrentVersion(Long documentId);

    KnowledgeDocument retire(Long documentId);
}
