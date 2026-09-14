package com.example.crackcs.learning.answer.service.result;

import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;

import java.util.List;

public record AnswerEvaluationResult(
        EvaluationStatus status,
        Verdict verdict,
        Integer score,
        String feedback,
        String failureReason,
        List<ConceptResult> concepts,
        List<String> strengths,
        List<String> omissions,
        List<String> misconceptions,
        List<EvidenceResult> evidence
) {
    public AnswerEvaluationResult(
            EvaluationStatus status, Verdict verdict, Integer score, String feedback,
            String failureReason, List<ConceptResult> concepts
    ) {
        this(status, verdict, score, feedback, failureReason, concepts, List.of(), List.of(), List.of(), List.of());
    }

    public static AnswerEvaluationResult from(Evaluation evaluation) {
        return new AnswerEvaluationResult(evaluation.getStatus(), evaluation.getVerdict(), evaluation.getScore(),
                evaluation.getFeedback(), evaluation.getFailureReason(), evaluation.getConcepts().stream()
                .map(concept -> new ConceptResult(concept.getConceptId(), concept.getConceptName(),
                        concept.getVerdict(), concept.getScore(),
                        concept.getFeedback())).toList(),
                List.copyOf(evaluation.getStrengths()),
                List.copyOf(evaluation.getOmissions()),
                List.copyOf(evaluation.getMisconceptions()),
                evaluation.getEvidence().stream().map(evidence -> new EvidenceResult(
                        evidence.getChunkId(), evidence.getDocumentTitle(), evidence.getDocumentVersion(),
                        evidence.getStartOffset(), evidence.getEndOffset(), evidence.getContent()
                )).toList());
    }

    public record ConceptResult(Long conceptId, String conceptName, Verdict verdict, Integer score, String feedback) {
    }

    public record EvidenceResult(
            Long chunkId,
            String documentTitle,
            int documentVersion,
            int startOffset,
            int endOffset,
            String content
    ) {
    }
}
