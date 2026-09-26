package com.example.crackcs.evaluation.retrieval;

import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeEvidenceSelector knowledgeEvidenceSelector;

    @Override
    public RetrievalResult retrieve(RetrievalQuery query, int limit) {
        return knowledgeEvidenceSelector.selectEvidence(
                knowledgeChunkRepository.findPublishedSearchableByTopicId(query.topicId()),
                query,
                limit
        );
    }
}
