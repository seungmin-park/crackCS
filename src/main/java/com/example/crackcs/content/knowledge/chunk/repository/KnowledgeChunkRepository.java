package com.example.crackcs.content.knowledge.chunk.repository;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
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
              AND document.status = com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus.PUBLISHED
              AND chunk.searchStatus <> com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunkSearchStatus.EMBEDDING_FAILED
            """)
    List<KnowledgeChunk> findPublishedSearchableByTopicId(@Param("topicId") Long topicId);
}
