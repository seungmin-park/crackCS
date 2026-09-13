package com.example.crackcs.learning.answer.service;

import com.example.crackcs.learning.answer.service.result.AnswerEvaluationResult;
import com.example.crackcs.learning.answer.service.result.AnswerResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnswerService {
    AnswerResult submit(Long memberId, Long questionId, String requestId, String content);

    AnswerResult findById(Long memberId, Long answerId);

    Page<AnswerResult> findAll(Long memberId, Pageable pageable);

    AnswerEvaluationResult findEvaluation(Long memberId, Long answerId);
}
