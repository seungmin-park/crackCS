package com.example.crackcs.learning.followup.controller.response;

import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;
import com.example.crackcs.learning.followup.service.FollowUpQuestionResult;

public record FollowUpQuestionResponse(FollowUpStatus status, FollowUpReason reason, QuestionResponse question) {
    public static FollowUpQuestionResponse from(FollowUpQuestionResult result) {
        FollowUpQuestionResult.QuestionResult question = result.question();
        return new FollowUpQuestionResponse(result.status(), result.reason(), question == null ? null :
                new QuestionResponse(question.id(), new TopicResponse(question.topic().id(), question.topic().code(),
                        question.topic().name()), question.difficulty(), question.content()));
    }

    public record QuestionResponse(Long id, TopicResponse topic, QuestionDifficulty difficulty, String content) {
    }

    public record TopicResponse(Long id, String code, String name) {
    }
}
