package com.example.crackcs.learning.controller;

import com.example.crackcs.auth.config.SecurityConfiguration;
import com.example.crackcs.auth.security.ApiAccessDeniedHandler;
import com.example.crackcs.auth.security.ApiAuthenticationEntryPoint;
import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.auth.security.SecurityErrorResponseWriter;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.exception.AnswerConflictException;
import com.example.crackcs.exception.AnswerNotFoundException;
import com.example.crackcs.learning.controller.response.AnswerResponse;
import com.example.crackcs.learning.controller.response.EvaluationResponse;
import com.example.crackcs.learning.service.AnswerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnswerController.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({
        SecurityConfiguration.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class
})
class AnswerControllerTest {

    private static final long MEMBER_ID = 41L;
    private static final String REQUEST_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnswerService answerService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("USER가 답변 원문을 제출하면 회원과 문제 정보를 전달하고 202를 반환한다")
    void submitsAnswer() throws Exception {
        AnswerResponse response = evaluatingAnswer(31L, 7L, "문제 본문", "  답변 원문  ");
        given(answerService.submit(MEMBER_ID, 7L, REQUEST_ID, "  답변 원문  ")).willReturn(response);
        Map<String, String> request = Map.of("requestId", REQUEST_ID, "content", "  답변 원문  ");

        mockMvc.perform(post("/api/questions/{questionId}/answers", 7L)
                        .with(user(userPrincipal()))
                        .with(csrf())
                        .contentType("application/json")
                        .header("Idempotency-Key", request.get("requestId"))
                        .content(objectMapper.writeValueAsString(Map.of("content", request.get("content")))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.answerId").value(31))
                .andExpect(jsonPath("$.questionId").value(7))
                .andExpect(jsonPath("$.questionContent").value("문제 본문"))
                .andExpect(jsonPath("$.content").value("  답변 원문  "))
                .andExpect(jsonPath("$.submittedAt").value("2026-09-07T10:30:00"))
                .andExpect(jsonPath("$.evaluation.status").value("EVALUATING"))
                .andExpect(jsonPath("$.evaluation.verdict").isEmpty())
                .andExpect(jsonPath("$.evaluation.concepts").isArray());

        verify(answerService).submit(MEMBER_ID, 7L, REQUEST_ID, "  답변 원문  ");
    }

    @Test
    @DisplayName("답변 이력을 기본 최신순과 기본 페이지 크기로 조회한다")
    void findsAnswerHistoryWithDefaultPaging() throws Exception {
        AnswerResponse response = evaluatingAnswer(31L, 7L, "문제 본문", "답변");
        given(answerService.findAll(eq(MEMBER_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/members/me/answers").with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].answerId").value(31))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(answerService).findAll(eq(MEMBER_ID), argThat(pageable ->
                pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 20
                        && pageable.getSort().getOrderFor("submittedAt") != null
                        && pageable.getSort().getOrderFor("submittedAt").isDescending()
                        && pageable.getSort().getOrderFor("id") != null
                        && pageable.getSort().getOrderFor("id").isDescending()
                        && pageable.getSort().getOrderFor("submittedAt").getProperty()
                        .equals(pageable.getSort().stream().findFirst().orElseThrow().getProperty())));
    }

    @Test
    @DisplayName("답변 이력의 page와 size를 Service에 전달한다")
    void findsAnswerHistoryWithRequestedPaging() throws Exception {
        given(answerService.findAll(eq(MEMBER_ID), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(2, 100), 0));

        mockMvc.perform(get("/api/members/me/answers")
                        .with(user(userPrincipal()))
                        .param("page", "2")
                        .param("size", "100"))
                .andExpect(status().isOk());

        verify(answerService).findAll(eq(MEMBER_ID), argThat(pageable ->
                pageable.getPageNumber() == 2 && pageable.getPageSize() == 100));
    }

    @Test
    @DisplayName("본인의 답변 ID를 조회하면 답변 상세를 반환한다")
    void findsAnswerById() throws Exception {
        AnswerResponse response = evaluatedAnswer();
        given(answerService.findById(MEMBER_ID, 31L)).willReturn(response);

        mockMvc.perform(get("/api/answers/{answerId}", 31L).with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").value(31))
                .andExpect(jsonPath("$.evaluation.status").value("EVALUATED"))
                .andExpect(jsonPath("$.evaluation.verdict").value("PARTIALLY_CORRECT"))
                .andExpect(jsonPath("$.evaluation.score").value(50))
                .andExpect(jsonPath("$.evaluation.feedback").value("핵심 개념을 보완하세요."));

        verify(answerService).findById(MEMBER_ID, 31L);
    }

    @Test
    @DisplayName("본인의 답변 평가를 조회하면 개념별 평가까지 반환한다")
    void findsEvaluation() throws Exception {
        EvaluationResponse response = evaluatedResponse();
        given(answerService.findEvaluation(MEMBER_ID, 31L)).willReturn(response);

        mockMvc.perform(get("/api/answers/{answerId}/evaluation", 31L).with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EVALUATED"))
                .andExpect(jsonPath("$.verdict").value("PARTIALLY_CORRECT"))
                .andExpect(jsonPath("$.score").value(50))
                .andExpect(jsonPath("$.concepts.length()").value(1))
                .andExpect(jsonPath("$.concepts[0].conceptId").value(11))
                .andExpect(jsonPath("$.concepts[0].verdict").value("CORRECT"))
                .andExpect(jsonPath("$.concepts[0].score").value(100))
                .andExpect(jsonPath("$.concepts[0].feedback").value("정확합니다."));

        verify(answerService).findEvaluation(MEMBER_ID, 31L);
    }

    @Test
    @DisplayName("실패한 평가는 실패 이유를 반환한다")
    void returnsFailedEvaluation() throws Exception {
        EvaluationResponse response = new EvaluationResponse(
                EvaluationStatus.FAILED, null, null, null, "평가 서버 시간 초과", List.of());
        given(answerService.findEvaluation(MEMBER_ID, 31L)).willReturn(response);

        mockMvc.perform(get("/api/answers/{answerId}/evaluation", 31L).with(user(userPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("평가 서버 시간 초과"))
                .andExpect(jsonPath("$.verdict").isEmpty())
                .andExpect(jsonPath("$.score").isEmpty())
                .andExpect(jsonPath("$.feedback").isEmpty())
                .andExpect(jsonPath("$.concepts").isArray());
    }

    @Test
    @DisplayName("존재하지 않는 답변 조회는 공통 404 오류를 반환한다")
    void returnsNotFoundForMissingAnswer() throws Exception {
        given(answerService.findById(MEMBER_ID, 999L)).willThrow(new AnswerNotFoundException(999L));

        mockMvc.perform(get("/api/answers/{answerId}", 999L).with(user(userPrincipal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANSWER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("답변을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("같은 요청 식별자로 다른 답변을 제출하면 공통 409 오류를 반환한다")
    void returnsConflictForReusedRequestId() throws Exception {
        given(answerService.submit(MEMBER_ID, 7L, REQUEST_ID, "다른 답변"))
                .willThrow(new AnswerConflictException());
        Map<String, String> request = Map.of("requestId", REQUEST_ID, "content", "다른 답변");

        mockMvc.perform(post("/api/questions/{questionId}/answers", 7L)
                        .with(user(userPrincipal()))
                        .with(csrf())
                        .contentType("application/json")
                        .header("Idempotency-Key", request.get("requestId"))
                        .content(objectMapper.writeValueAsString(Map.of("content", request.get("content")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANSWER_CONFLICT"))
                .andExpect(jsonPath("$.message").value("같은 요청 식별자로 다른 답변을 제출할 수 없습니다."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("요청 식별자가 UUID 형식이 아니면 Service를 호출하지 않고 400을 반환한다")
    void rejectsMalformedRequestId() throws Exception {
        assertInvalidIdempotencyKey("not-a-uuid");
    }

    @Test
    @DisplayName("요청 식별자가 대문자 UUID이면 Service를 호출하지 않고 400을 반환한다")
    void rejectsNonCanonicalUppercaseRequestId() throws Exception {
        assertInvalidIdempotencyKey(REQUEST_ID.toUpperCase());
    }

    @Test
    @DisplayName("답변이 공백이면 Service를 호출하지 않고 400을 반환한다")
    void rejectsBlankContent() throws Exception {
        assertInvalidSubmission(Map.of("requestId", REQUEST_ID, "content", "   "), "content");
    }

    @Test
    @DisplayName("답변이 10000자를 넘으면 Service를 호출하지 않고 400을 반환한다")
    void rejectsContentOverMaximumLength() throws Exception {
        assertInvalidSubmission(Map.of("requestId", REQUEST_ID, "content", "가".repeat(10_001)), "content");
    }

    @Test
    @DisplayName("문제 ID가 양수가 아니면 Service를 호출하지 않고 400을 반환한다")
    void rejectsNonPositiveQuestionId() throws Exception {
        Map<String, String> request = Map.of("requestId", REQUEST_ID, "content", "답변");

        mockMvc.perform(post("/api/questions/{questionId}/answers", 0)
                        .with(user(userPrincipal()))
                        .with(csrf())
                        .contentType("application/json")
                        .header("Idempotency-Key", request.get("requestId"))
                        .content(objectMapper.writeValueAsString(Map.of("content", request.get("content")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("questionId"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("답변 ID가 양수가 아니면 Service를 호출하지 않고 400을 반환한다")
    void rejectsNonPositiveAnswerId() throws Exception {
        mockMvc.perform(get("/api/answers/{answerId}", 0).with(user(userPrincipal())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("answerId"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("page가 음수이면 Service를 호출하지 않고 400을 반환한다")
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/members/me/answers")
                        .with(user(userPrincipal()))
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("size가 100을 넘으면 Service를 호출하지 않고 400을 반환한다")
    void rejectsSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/members/me/answers")
                        .with(user(userPrincipal()))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("size가 0이면 Service를 호출하지 않고 400을 반환한다")
    void rejectsSizeBelowMinimum() throws Exception {
        mockMvc.perform(get("/api/members/me/answers")
                        .with(user(userPrincipal()))
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("비로그인 사용자의 답변 조회를 401로 거부한다")
    void rejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/answers/{answerId}", 31L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("CSRF 토큰 없는 답변 제출을 403으로 거부한다")
    void rejectsSubmissionWithoutCsrfToken() throws Exception {
        Map<String, String> request = Map.of("requestId", REQUEST_ID, "content", "답변");

        mockMvc.perform(post("/api/questions/{questionId}/answers", 7L)
                        .with(user(userPrincipal()))
                        .contentType("application/json")
                        .header("Idempotency-Key", request.get("requestId"))
                        .content(objectMapper.writeValueAsString(Map.of("content", request.get("content")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("ADMIN의 답변 제출을 403으로 거부한다")
    void rejectsAdminSubmission() throws Exception {
        Map<String, String> request = Map.of("requestId", REQUEST_ID, "content", "답변");

        mockMvc.perform(post("/api/questions/{questionId}/answers", 7L)
                        .with(user(adminPrincipal()))
                        .with(csrf())
                        .contentType("application/json")
                        .header("Idempotency-Key", request.get("requestId"))
                        .content(objectMapper.writeValueAsString(Map.of("content", request.get("content")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("ADMIN의 답변 이력 조회를 403으로 거부한다")
    void rejectsAdminHistoryRequest() throws Exception {
        mockMvc.perform(get("/api/members/me/answers").with(user(adminPrincipal())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(answerService);
    }

    @Test
    @DisplayName("멱등 키는 헤더로 받고 제출 응답에 답변과 평가 식별자를 포함한다")
    void acceptsDocumentedIdempotencyHeader() throws Exception {
        given(answerService.submit(MEMBER_ID, 7L, REQUEST_ID, "답변"))
                .willReturn(evaluatingAnswer(31L, 7L, "질문", "답변"));
        mockMvc.perform(post("/api/questions/{questionId}/answers", 7L)
                        .with(user(userPrincipal())).with(csrf())
                        .header("Idempotency-Key", REQUEST_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("content", "답변"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.answerId").value(31L))
                .andExpect(jsonPath("$.evaluationId").isNumber());
    }

    private void assertInvalidIdempotencyKey(String key) throws Exception {
        mockMvc.perform(post("/api/questions/7/answers").with(user(userPrincipal())).with(csrf())
                        .header("Idempotency-Key", key).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("content", "답변"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Idempotency-Key는 소문자 UUID 형식이어야 합니다."));
        verifyNoInteractions(answerService);
    }

    private void assertInvalidSubmission(Map<String, String> request, String field) throws Exception {
        mockMvc.perform(post("/api/questions/{questionId}/answers", 7L)
                        .with(user(userPrincipal()))
                        .with(csrf())
                        .contentType("application/json")
                        .header("Idempotency-Key", request.get("requestId"))
                        .content(objectMapper.writeValueAsString(Map.of("content", request.get("content")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));

        verifyNoInteractions(answerService);
    }

    private AnswerResponse evaluatingAnswer(Long id, Long questionId, String questionContent, String content) {
        EvaluationResponse evaluation = new EvaluationResponse(
                EvaluationStatus.EVALUATING, null, null, null, null, List.of());
        return new AnswerResponse(
                id, questionId, questionContent, content,
                LocalDateTime.of(2026, 9, 7, 10, 30), evaluation, 51L);
    }

    private AnswerResponse evaluatedAnswer() {
        return new AnswerResponse(
                31L, 7L, "문제 본문", "답변 원문",
                LocalDateTime.of(2026, 9, 7, 10, 30), evaluatedResponse(), 51L);
    }

    private EvaluationResponse evaluatedResponse() {
        return new EvaluationResponse(
                EvaluationStatus.EVALUATED,
                Verdict.PARTIALLY_CORRECT,
                50,
                "핵심 개념을 보완하세요.",
                null,
                List.of(new EvaluationResponse.ConceptResponse(
                        11L, "스레드", Verdict.CORRECT, 100, "정확합니다."))
        );
    }

    private AuthenticatedMember userPrincipal() {
        return principal("user@example.com", "ROLE_USER");
    }

    private AuthenticatedMember adminPrincipal() {
        return principal("admin@example.com", "ROLE_ADMIN");
    }

    private AuthenticatedMember principal(String username, String authority) {
        AuthenticatedMember principal = mock(AuthenticatedMember.class);
        given(principal.memberId()).willReturn(MEMBER_ID);
        given(principal.getUsername()).willReturn(username);
        given(principal.getPassword()).willReturn("encoded");
        org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority(authority)))
                .when(principal).getAuthorities();
        given(principal.isEnabled()).willReturn(true);
        return principal;
    }
}
