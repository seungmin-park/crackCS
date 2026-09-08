package com.example.crackcs.learning.controller.response;

import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;

import java.util.List;

public record EvaluationResponse(
        EvaluationStatus status,
        Verdict verdict,
        Integer score,
        String feedback,
        String failureReason,
        List<ConceptResponse> concepts,
        List<String> strengths,
        List<String> omissions,
        List<String> misconceptions,
        List<EvidenceResponse> evidence
) {
    public EvaluationResponse(
            EvaluationStatus status, Verdict verdict, Integer score, String feedback,
            String failureReason, List<ConceptResponse> concepts
    ) {
        this(status, verdict, score, feedback, failureReason, concepts, List.of(), List.of(), List.of(), List.of());
    }

    public static EvaluationResponse from(Evaluation evaluation) {
        return new EvaluationResponse(evaluation.getStatus(), evaluation.getVerdict(), evaluation.getScore(),
                evaluation.getFeedback(), evaluation.getFailureReason(), evaluation.getConcepts().stream()
                .map(c -> new ConceptResponse(c.getConceptId(), c.getConceptName(), c.getVerdict(), c.getScore(),
                        c.getFeedback())).toList(),
                evaluation.getStrengths(),
                evaluation.getOmissions(),
                evaluation.getMisconceptions(),
                evaluation.getEvidence().stream().map(e -> new EvidenceResponse(
                        e.getChunkId(), e.getDocumentTitle(), e.getDocumentVersion(),
                        e.getStartOffset(), e.getEndOffset(), e.getContent()
                )).toList());
    }

    public record ConceptResponse(Long conceptId, String conceptName, Verdict verdict, Integer score, String feedback) {
    }

    public record EvidenceResponse(
            Long chunkId,
            String documentTitle,
            int documentVersion,
            int startOffset,
            int endOffset,
            String content
    ) {
    }
}
