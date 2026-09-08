package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminEvaluationService {
    Page<AdminEvaluationSummary> findFailures(EvaluationStatus status, Pageable pageable);

    AdminEvaluationDetail findFailureById(Long evaluationId);
}
