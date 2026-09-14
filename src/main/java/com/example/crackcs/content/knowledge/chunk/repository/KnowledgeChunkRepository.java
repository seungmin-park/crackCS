package com.example.crackcs.content.knowledge.chunk.repository;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunkSearchStatus;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findAllByDocument_IdOrderBySequenceNo(Long documentId);

    @Query("""
            SELECT chunk
            FROM KnowledgeChunk chunk
            JOIN FETCH chunk.document document
            JOIN FETCH document.topic
            WHERE document.topic.id = :topicId
              AND document.status = :status
              AND chunk.searchStatus <> :excludedSearchStatus
            """)
    List<KnowledgeChunk> findSearchableByTopicId(@Param("topicId") Long topicId,
                                                 @Param("status") KnowledgeDocumentStatus status,
                                                 @Param("excludedSearchStatus") KnowledgeChunkSearchStatus excludedSearchStatus);

    default List<KnowledgeChunk> findPublishedSearchableByTopicId(Long topicId) {
        return findSearchableByTopicId(topicId, KnowledgeDocumentStatus.PUBLISHED,
                KnowledgeChunkSearchStatus.EMBEDDING_FAILED);
    }
}
