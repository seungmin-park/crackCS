package com.example.crackcs.content.knowledge.chunk.domain;

import com.example.crackcs.content.knowledge.domain.ContentChecksum;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.exception.InvalidContentStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "knowledge_chunk",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_knowledge_chunk_document_sequence", columnNames = {"document_id", "sequence_no"}
        ),
        indexes = {
                @Index(name = "idx_knowledge_chunk_document", columnList = "document_id"),
                @Index(name = "idx_knowledge_chunk_search_status", columnList = "search_status")
        }
)
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDocument document;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "start_offset", nullable = false)
    private int startOffset;

    @Column(name = "end_offset", nullable = false)
    private int endOffset;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "chunk_policy_version", nullable = false, length = 30)
    private String chunkPolicyVersion;

    @Column(name = "generation_key", nullable = false, length = 128)
    private String generationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_status", nullable = false, length = 30)
    private KnowledgeChunkSearchStatus searchStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static KnowledgeChunk create(
            KnowledgeDocument document,
            int sequenceNo,
            int startOffset,
            int endOffset,
            String content,
            String chunkPolicyVersion
    ) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (document.getStatus() != KnowledgeDocumentStatus.PUBLISHED) {
            throw new InvalidContentStateException("PUBLISHED 문서만 Chunk를 생성할 수 있습니다.");
        }
        if (sequenceNo < 0) {
            throw new IllegalArgumentException("sequenceNo must not be negative");
        }
        if (startOffset < 0 || endOffset <= startOffset || endOffset > document.getContent().length()) {
            throw new IllegalArgumentException("chunk offsets must identify source content");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (!document.getContent().substring(startOffset, endOffset).equals(content)) {
            throw new IllegalArgumentException("chunk content must match source offsets");
        }
        if (chunkPolicyVersion == null || chunkPolicyVersion.isBlank()) {
            throw new IllegalArgumentException("chunkPolicyVersion must not be blank");
        }

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.document = document;
        chunk.sequenceNo = sequenceNo;
        chunk.startOffset = startOffset;
        chunk.endOffset = endOffset;
        chunk.content = content;
        chunk.checksum = ContentChecksum.sha256(content);
        chunk.chunkPolicyVersion = chunkPolicyVersion.trim();
        chunk.generationKey = ContentChecksum.sha256(document.getChecksum() + ":" + chunk.chunkPolicyVersion);
        chunk.searchStatus = KnowledgeChunkSearchStatus.KEYWORD_SEARCHABLE;
        chunk.createdAt = LocalDateTime.now();
        return chunk;
    }

    public Long getDocumentId() {
        return document.getId();
    }
}
