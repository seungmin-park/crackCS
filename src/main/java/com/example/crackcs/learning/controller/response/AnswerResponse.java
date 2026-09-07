package com.example.crackcs.learning.controller.response;

import com.example.crackcs.learning.domain.Answer;
import com.example.crackcs.evaluation.domain.Evaluation;
import java.time.LocalDateTime;

public record AnswerResponse(Long answerId, Long questionId, String questionContent, String content,
        LocalDateTime submittedAt, EvaluationResponse evaluation, Long evaluationId) {
    public static AnswerResponse from(Answer answer, Evaluation evaluation) {
        return new AnswerResponse(answer.getId(), answer.getQuestion().getId(), answer.getQuestion().getContent(),
                answer.getContent(), answer.getSubmittedAt(), EvaluationResponse.from(evaluation), evaluation.getId());
    }
}
