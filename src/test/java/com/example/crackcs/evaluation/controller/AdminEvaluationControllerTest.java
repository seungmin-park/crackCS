package com.example.crackcs.evaluation.controller;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.service.AdminEvaluationDetail;
import com.example.crackcs.evaluation.service.AdminEvaluationService;
import com.example.crackcs.evaluation.service.AdminEvaluationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEvaluationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminEvaluationControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    AdminEvaluationService service;

    @Test
    @DisplayName("관리자는 검토 필요 평가를 상태로 필터링해 조회한다")
    void findsReviewEvaluations() throws Exception {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 8, 10, 0);
        PageRequest pageable = PageRequest.of(0, 20,
                Sort.by(Sort.Order.desc("evaluatedAt"), Sort.Order.desc("id")));
        given(service.findFailures(EvaluationStatus.NEEDS_REVIEW, pageable))
                .willReturn(new PageImpl<>(List.of(new AdminEvaluationSummary(
                        5L, 7L, 9L, EvaluationStatus.NEEDS_REVIEW,
                        "EVIDENCE_NOT_FOUND", "gpt-5.6-terra", "os-evaluator-v1", occurredAt
                )), pageable, 1));

        mockMvc.perform(get("/api/admin/evaluations").param("status", "NEEDS_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].evaluationId").value(5L))
                .andExpect(jsonPath("$.content[0].failureCode").value("EVIDENCE_NOT_FOUND"))
                .andExpect(jsonPath("$.content[0].evaluatorVersion").value("os-evaluator-v1"));

        verify(service).findFailures(EvaluationStatus.NEEDS_REVIEW, pageable);
    }

    @Test
    @DisplayName("관리자 평가 상세는 답변 원문과 근거 범위를 반환한다")
    void findsEvaluationDetailWithOriginalAnswer() throws Exception {
        given(service.findFailureById(5L)).willReturn(new AdminEvaluationDetail(
                5L, 7L, 9L, "프로세스를 설명하세요", "사용자 원문",
                EvaluationStatus.FAILED, "PROVIDER_TIMEOUT", "gpt-5.6-terra", "os-evaluator-v1",
                LocalDateTime.of(2026, 9, 8, 10, 0), List.of()
        ));

        mockMvc.perform(get("/api/admin/evaluations/{evaluationId}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerContent").value("사용자 원문"))
                .andExpect(jsonPath("$.failureCode").value("PROVIDER_TIMEOUT"));
    }

    @Test
    @DisplayName("관리자 실패 목록은 성공 상태 필터를 거부한다")
    void rejectsSuccessStatusFilter() throws Exception {
        mockMvc.perform(get("/api/admin/evaluations").param("status", "EVALUATED"))
                .andExpect(status().isBadRequest());
    }
}
