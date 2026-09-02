package com.example.crackcs.content.knowledge.service;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface KnowledgeDocumentService {

    KnowledgeDocument create(Long creatorMemberId, KnowledgeDocumentData data);

    Page<KnowledgeDocument> findAll(
            Long topicId,
            KnowledgeDocumentStatus status,
            String technologyVersion,
            Pageable pageable
    );

    KnowledgeDocument findById(Long documentId);

    KnowledgeDocument update(Long documentId, KnowledgeDocumentData data);

    KnowledgeDocument createNextVersion(Long documentId, Long creatorMemberId, KnowledgeDocumentData data);

    KnowledgeDocument review(Long documentId, Long reviewerMemberId);

    KnowledgeDocument publish(Long documentId);

    KnowledgeDocument retire(Long documentId);

    List<KnowledgeDocument> findPublishedCandidates(Long topicId);
}
