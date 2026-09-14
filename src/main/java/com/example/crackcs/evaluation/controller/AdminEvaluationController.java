package com.example.crackcs.evaluation.controller;

import com.example.crackcs.common.web.response.PageResponse;
import com.example.crackcs.evaluation.controller.request.AdminEvaluationSearchRequest;
import com.example.crackcs.evaluation.controller.response.AdminEvaluationDetailResponse;
import com.example.crackcs.evaluation.controller.response.AdminEvaluationSummaryResponse;
import com.example.crackcs.evaluation.service.AdminEvaluationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/evaluations")
public class AdminEvaluationController {

    private final AdminEvaluationService adminEvaluationService;

    @GetMapping
    public PageResponse<AdminEvaluationSummaryResponse> findFailures(
            @Valid @ModelAttribute AdminEvaluationSearchRequest request
    ) {
        return PageResponse.from(adminEvaluationService.findFailures(request.statusValue(), request.pageable()),
                AdminEvaluationSummaryResponse::from);
    }

    @GetMapping("/{evaluationId}")
    public AdminEvaluationDetailResponse findFailureById(
            @Positive(message = "evaluationId는 양수여야 합니다.") @PathVariable Long evaluationId
    ) {
        return AdminEvaluationDetailResponse.from(adminEvaluationService.findFailureById(evaluationId));
    }
}
