package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationResult;

import java.util.List;

sealed interface EvaluationAttemptOutcome {

    record Completed(EvaluationResult evaluationResult, List<Long> evidenceChunkIds)
            implements EvaluationAttemptOutcome {
        public Completed {
            evidenceChunkIds = List.copyOf(evidenceChunkIds);
        }
    }

    record ReviewRequired(String reason) implements EvaluationAttemptOutcome {
    }

    record RetryRequired(String reason) implements EvaluationAttemptOutcome {
    }
}
