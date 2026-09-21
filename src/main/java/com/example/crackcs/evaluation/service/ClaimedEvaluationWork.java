package com.example.crackcs.evaluation.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.evaluation.port.EvaluationConceptInput;
import com.example.crackcs.evaluation.port.EvaluationEvidenceInput;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.retrieval.RetrievalQuery;
import com.example.crackcs.evaluation.retrieval.RetrievalResult;

import java.util.List;

record ClaimedEvaluationWork(
        Long evaluationId,
        Long answerId,
        Long memberId,
        Long topicId,
        String question,
        String referenceAnswer,
        String answer,
        List<EvaluationConceptInput> concepts,
        int attemptCount
) {

    RetrievalQuery retrievalQuery() {
        return new RetrievalQuery(
                topicId,
                concepts.stream().map(EvaluationConceptInput::name).toList(),
                question,
                referenceAnswer,
                answer
        );
    }

    EvaluationRequest request(RetrievalResult retrieval) {
        List<EvaluationEvidenceInput> evidence = retrieval.chunks().stream()
                .map(retrieved -> toEvidenceInput(retrieved.chunk(), retrieved.relevanceScore()))
                .toList();
        return new EvaluationRequest(topicId, question, referenceAnswer, answer, concepts, evidence);
    }

    private EvaluationEvidenceInput toEvidenceInput(KnowledgeChunk chunk, double relevanceScore) {
        KnowledgeDocument document = chunk.getDocument();
        return new EvaluationEvidenceInput(
                chunk.getId(),
                document.getId(),
                document.getTitle(),
                document.getDocumentVersion(),
                chunk.getStartOffset(),
                chunk.getEndOffset(),
                chunk.getContent(),
                relevanceScore
        );
    }
}
