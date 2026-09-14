package com.example.crackcs.evaluation.repository;

import com.example.crackcs.evaluation.domain.Evaluation;

import java.util.List;

public interface EvaluationDetailsRepository {
    /**
     * Both collection fetches run in the caller's persistence context; returned evaluations contain both details.
     */
    List<Evaluation> findAllDetailsByAnswerIds(List<Long> answerIds);
}
