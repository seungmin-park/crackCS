package com.example.crackcs.evaluation.port;

import com.example.crackcs.evaluation.domain.Verdict;
import java.util.List;

public record EvaluationResult(Verdict verdict, String feedback, List<ConceptResult> concepts) {
    public EvaluationResult { concepts = concepts == null ? null : List.copyOf(concepts); }
}
