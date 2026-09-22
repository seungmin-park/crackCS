package com.example.crackcs.learning.followup.service;

import com.example.crackcs.learning.followup.domain.FollowUpGenerationResult;
import com.example.crackcs.learning.followup.domain.FollowUpReason;

sealed interface FollowUpGenerationAttemptOutcome {

    record Completed(FollowUpGenerationResult generationResult) implements FollowUpGenerationAttemptOutcome {
    }

    record RetryRequired(FollowUpReason reason) implements FollowUpGenerationAttemptOutcome {
    }

    record FailureRequired(FollowUpReason reason) implements FollowUpGenerationAttemptOutcome {
    }
}
