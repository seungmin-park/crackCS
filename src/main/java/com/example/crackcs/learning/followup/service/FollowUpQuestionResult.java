package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;

public record FollowUpQuestionResult(FollowUpStatus status, FollowUpReason reason, QuestionResult question) {
    public FollowUpQuestionResult {
        if (status == null) {
            throw new IllegalArgumentException("follow-up status is required");
        }
        requireQuestionForReadyState(status, question);
        requireReasonForFailureState(status, reason);
    }

    private static void requireQuestionForReadyState(FollowUpStatus status, QuestionResult question) {
        if (status == FollowUpStatus.READY && question == null) {
            throw new IllegalArgumentException("READY results require a question");
        }
        if (status != FollowUpStatus.READY && question != null) {
            throw new IllegalArgumentException("only READY results may contain a question");
        }
    }

    private static void requireReasonForFailureState(FollowUpStatus status, FollowUpReason reason) {
        if (status == FollowUpStatus.READY && reason != null) {
            throw new IllegalArgumentException("READY results must not contain a failure reason");
        }
        if (requiresReason(status) && reason == null) {
            throw new IllegalArgumentException("failed or unavailable results require a reason");
        }
    }

    private static boolean requiresReason(FollowUpStatus status) {
        return status == FollowUpStatus.FAILED || status == FollowUpStatus.UNAVAILABLE;
    }

    public static FollowUpQuestionResult pending() {
        return withoutQuestion(FollowUpStatus.PENDING, null);
    }

    public static FollowUpQuestionResult unavailable(FollowUpReason reason) {
        return withoutQuestion(FollowUpStatus.UNAVAILABLE, reason);
    }

    public static FollowUpQuestionResult withoutQuestion(FollowUpStatus status, FollowUpReason reason) {
        return new FollowUpQuestionResult(status, reason, null);
    }

    public static FollowUpQuestionResult ready(QuestionResult question) {
        return new FollowUpQuestionResult(FollowUpStatus.READY, null, question);
    }

    public record QuestionResult(Long id, TopicResult topic, QuestionDifficulty difficulty, String content) {
    }

    public record TopicResult(Long id, String code, String name) {
    }
}
