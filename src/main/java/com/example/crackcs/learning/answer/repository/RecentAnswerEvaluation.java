package com.example.crackcs.learning.answer.repository;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;
import java.time.LocalDateTime;

public interface RecentAnswerEvaluation {
    Long getAnswerId();

    String getQuestionTitle();

    EvaluationStatus getStatus();

    Verdict getVerdict();

    Integer getScore();

    LocalDateTime getSubmittedAt();
}
