package com.example.crackcs.evaluation.port;

import com.example.crackcs.evaluation.domain.Verdict;
import java.util.List;

public record EvaluationResult(
        Verdict verdict,
        String feedback,
        List<ConceptResult> concepts,
        List<String> strengths,
        List<String> omissions,
        List<String> misconceptions,
        List<Long> evidenceChunkIds,
        String modelName,
        String evaluatorVersion,
        long durationMillis,
        long inputTokens,
        long outputTokens
) {
    public EvaluationResult {
        concepts = concepts == null ? null : List.copyOf(concepts);
        strengths = strengths == null ? null : List.copyOf(strengths);
        omissions = omissions == null ? null : List.copyOf(omissions);
        misconceptions = misconceptions == null ? null : List.copyOf(misconceptions);
        evidenceChunkIds = evidenceChunkIds == null ? null : List.copyOf(evidenceChunkIds);
    }

    public EvaluationResult(Verdict verdict, String feedback, List<ConceptResult> concepts) {
        this(verdict, feedback, concepts, List.of(), List.of(), List.of(), List.of(),
                "stub", "phase4", 0, 0, 0);
    }
}
