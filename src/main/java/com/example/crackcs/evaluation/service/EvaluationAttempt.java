package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationResult;

import java.util.List;

sealed interface EvaluationAttempt {

    record Completed(EvaluationResult result, List<Long> evidenceChunkIds) implements EvaluationAttempt {
        public Completed {
            evidenceChunkIds = List.copyOf(evidenceChunkIds);
        }
    }

    record ReviewRequired(String reason) implements EvaluationAttempt {
    }

    record RetryRequired(String reason) implements EvaluationAttempt {
    }
}
