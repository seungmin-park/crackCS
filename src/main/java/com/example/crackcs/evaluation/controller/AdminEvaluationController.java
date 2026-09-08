package com.example.crackcs.evaluation.controller;

import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.evaluation.controller.request.AdminEvaluationSearchRequest;
import com.example.crackcs.evaluation.controller.response.AdminEvaluationDetailResponse;
import com.example.crackcs.evaluation.controller.response.AdminEvaluationSummaryResponse;
import com.example.crackcs.evaluation.service.AdminEvaluationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/evaluations")
public class AdminEvaluationController {

    private final AdminEvaluationService service;

    @GetMapping
    public PageResponse<AdminEvaluationSummaryResponse> findFailures(
            @Valid @ModelAttribute AdminEvaluationSearchRequest request
    ) {
        return PageResponse.from(service.findFailures(request.statusValue(), request.pageable()),
                AdminEvaluationSummaryResponse::from);
    }

    @GetMapping("/{evaluationId}")
    public AdminEvaluationDetailResponse findFailureById(
            @Positive(message = "evaluationId는 양수여야 합니다.") @PathVariable Long evaluationId
    ) {
        return AdminEvaluationDetailResponse.from(service.findFailureById(evaluationId));
    }
}
