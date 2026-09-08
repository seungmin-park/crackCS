package com.example.crackcs.learning.service;

import com.example.crackcs.learning.controller.response.AnswerResponse;
import com.example.crackcs.learning.controller.response.EvaluationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnswerService {
    AnswerResponse submit(Long memberId, Long questionId, String requestId, String content);

    AnswerResponse findById(Long memberId, Long answerId);

    Page<AnswerResponse> findAll(Long memberId, Pageable pageable);

    EvaluationResponse findEvaluation(Long memberId, Long answerId);
}
