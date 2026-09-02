package com.example.crackcs.content.knowledge.repository;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    boolean existsByChecksum(String checksum);

    boolean existsByChecksumAndIdNot(String checksum, Long id);

    List<KnowledgeDocument> findAllByVersionSeriesIdAndStatus(
            String versionSeriesId,
            KnowledgeDocumentStatus status
    );

    @Query("""
            SELECT COALESCE(MAX(document.documentVersion), 0)
            FROM KnowledgeDocument document
            WHERE document.versionSeriesId = :versionSeriesId
            """)
    int findMaxVersion(@Param("versionSeriesId") String versionSeriesId);

    @Query("""
            SELECT document
            FROM KnowledgeDocument document
            WHERE (:topicId IS NULL OR document.topic.id = :topicId)
              AND (:status IS NULL OR document.status = :status)
              AND (:technologyVersion IS NULL OR document.technologyVersion = :technologyVersion)
            """)
    Page<KnowledgeDocument> findAllByConditions(
            @Param("topicId") Long topicId,
            @Param("status") KnowledgeDocumentStatus status,
            @Param("technologyVersion") String technologyVersion,
            Pageable pageable
    );

    @Query("""
            SELECT document
            FROM KnowledgeDocument document
            WHERE document.topic.id = :topicId
              AND document.status = com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus.PUBLISHED
            ORDER BY document.documentVersion DESC, document.id DESC
            """)
    List<KnowledgeDocument> findPublishedCandidatesByTopicId(@Param("topicId") Long topicId);
}
