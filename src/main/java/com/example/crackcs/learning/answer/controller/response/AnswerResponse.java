package com.example.crackcs.learning.answer.controller.response;

import com.example.crackcs.common.web.response.ApiTimestamp;
import com.example.crackcs.learning.answer.service.result.AnswerResult;

import java.time.Instant;

public record AnswerResponse(Long answerId, Long questionId, String questionContent, String content,
                             Instant submittedAt, EvaluationResponse evaluation, Long evaluationId) {
    public static AnswerResponse from(AnswerResult result) {
        return new AnswerResponse(result.answerId(), result.questionId(), result.questionContent(),
                result.content(), ApiTimestamp.from(result.submittedAt()), EvaluationResponse.from(result.evaluation()),
                result.evaluationId());
    }
}
