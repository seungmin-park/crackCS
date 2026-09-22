package com.example.crackcs.content.knowledge.chunk.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
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

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkPolicy chunkPolicy;

    @Override
    @Transactional
    public ChunkGenerationResult generateChunks(Long documentId) {
        KnowledgeDocument document = findPublishedDocument(documentId);
        List<KnowledgeChunk> existingChunks = knowledgeChunkRepository
                .findAllByDocument_IdOrderBySequenceNo(documentId);
        if (!existingChunks.isEmpty()) {
            return new ChunkGenerationResult(
                    existingChunks.getFirst().getGenerationKey(),
                    true,
                    existingChunks
            );
        }

        List<KnowledgeChunk> createdChunks = chunkPolicy.split(document.getContent()).stream()
                .map(slice -> KnowledgeChunk.create(
                        document,
                        slice.sequenceNo(),
                        slice.startOffset(),
                        slice.endOffset(),
                        slice.content(),
                        chunkPolicy.policyVersion()
                ))
                .toList();
        List<KnowledgeChunk> savedChunks = knowledgeChunkRepository.saveAll(createdChunks);
        return new ChunkGenerationResult(
                chunkPolicy.generationKey(document.getChecksum()),
                false,
                savedChunks
        );
    }

    private KnowledgeDocument findPublishedDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(documentId));
        if (document.getStatus() != KnowledgeDocumentStatus.PUBLISHED) {
            throw new InvalidContentStateException("PUBLISHED 문서만 Chunk를 생성할 수 있습니다.");
        }
        return document;
    }

    @Override
    public List<KnowledgeChunk> findByDocumentId(Long documentId) {
        if (!knowledgeDocumentRepository.existsById(documentId)) {
            throw new KnowledgeDocumentNotFoundException(documentId);
        }
        return knowledgeChunkRepository.findAllByDocument_IdOrderBySequenceNo(documentId);
    }
}
