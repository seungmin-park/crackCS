package com.example.crackcs.learning.answer.service.result;

import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.learning.answer.domain.Answer;

import java.time.LocalDateTime;

public record AnswerResult(Long answerId, Long questionId, String questionContent, String content,
                           LocalDateTime submittedAt, AnswerEvaluationResult evaluation, Long evaluationId) {
    public static AnswerResult from(Answer answer, Evaluation evaluation) {
        return new AnswerResult(answer.getId(), answer.getQuestion().getId(), answer.getQuestion().getContent(),
                answer.getContent(), answer.getSubmittedAt(), AnswerEvaluationResult.from(evaluation),
                evaluation.getId());
    }
}
