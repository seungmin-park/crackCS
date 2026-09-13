package com.example.crackcs.learning.answer.controller.response;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.learning.answer.service.result.AnswerEvaluationResult;
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
    public static EvaluationResponse from(AnswerEvaluationResult result) {
        return new EvaluationResponse(result.status(), result.verdict(), result.score(),
                result.feedback(), result.failureReason(), result.concepts().stream()
                .map(concept -> new ConceptResponse(concept.conceptId(), concept.conceptName(), concept.verdict(),
                        concept.score(),
                        concept.feedback())).toList(),
                result.strengths(), result.omissions(), result.misconceptions(),
                result.evidence().stream().map(evidence -> new EvidenceResponse(
                        evidence.chunkId(), evidence.documentTitle(), evidence.documentVersion(),
                        evidence.startOffset(), evidence.endOffset(), evidence.content()
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
