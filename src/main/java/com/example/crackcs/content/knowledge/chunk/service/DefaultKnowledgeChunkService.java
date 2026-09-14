package com.example.crackcs.content.knowledge.chunk.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.domain.ContentChecksum;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.exception.KnowledgeDocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultKnowledgeChunkService implements KnowledgeChunkService {

    static final String POLICY_VERSION = "paragraph-1000-overlap-150-v1";

    private final KnowledgeChunkRepository chunks;
    private final KnowledgeDocumentRepository documents;

    @Override
    @Transactional
    public ChunkGenerationResult generateChunks(Long documentId) {
        KnowledgeDocument document = documents.findById(documentId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(documentId));
        if (document.getStatus() != KnowledgeDocumentStatus.PUBLISHED) {
            throw new InvalidContentStateException("PUBLISHED 문서만 Chunk를 생성할 수 있습니다.");
        }
        String generationKey = generationKey(document);
        List<KnowledgeChunk> existing = chunks.findAllByDocument_IdOrderBySequenceNo(documentId);
        if (!existing.isEmpty()) {
            return new ChunkGenerationResult(existing.getFirst().getGenerationKey(), true, existing);
        }
        KnowledgeChunker chunker = new KnowledgeChunker(1000, 150);
        List<KnowledgeChunk> created = chunker.split(document.getContent()).stream()
                .map(slice -> KnowledgeChunk.create(
                        document,
                        slice.sequenceNo(),
                        slice.startOffset(),
                        slice.endOffset(),
                        slice.content(),
                        POLICY_VERSION
                ))
                .toList();
        return new ChunkGenerationResult(generationKey, false, chunks.saveAll(created));
    }

    @Override
    public List<KnowledgeChunk> findByDocumentId(Long documentId) {
        if (!documents.existsById(documentId)) {
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        return chunks.findAllByDocument_IdOrderBySequenceNo(documentId);
    }

    private String generationKey(KnowledgeDocument document) {
        return ContentChecksum.sha256(document.getChecksum() + ":" + POLICY_VERSION);
    }
}
