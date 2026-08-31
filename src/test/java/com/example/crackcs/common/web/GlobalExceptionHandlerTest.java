package com.example.crackcs.common.web;

import com.example.crackcs.content.question.controller.QuestionController;
import com.example.crackcs.content.question.controller.request.QuestionCreateRequest;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.content.question.service.QuestionService;
import com.example.crackcs.exception.TopicNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.FailureController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuestionService questionService;

    @Test
    @DisplayName("존재하지 않는 문제 예외를 공통 404 응답으로 변환한다")
    void handlesQuestionNotFoundException() throws Exception {
        given(questionService.findById(999999L))
                .willThrow(new QuestionNotFoundException(999999L));

        mockMvc.perform(get("/api/admin/questions/{questionId}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 주제 예외를 공통 404 응답으로 변환한다")
    void handlesTopicNotFoundException() throws Exception {
        QuestionCreateRequest request = new QuestionCreateRequest(
                999999L,
                "BASIC",
                "프로세스란 무엇인가요?",
                "모범 답안"
        );
        given(questionService.create(
                999999L,
                QuestionDifficulty.BASIC,
                "프로세스란 무엇인가요?",
                "모범 답안"
        )).willThrow(new TopicNotFoundException(999999L));

        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("DTO 검증 실패를 필드 오류가 포함된 공통 400 응답으로 변환한다")
    void handlesRequestValidationException() throws Exception {
        QuestionCreateRequest request = new QuestionCreateRequest(
                1L,
                "BASIC",
                " ",
                "모범 답안"
        );

        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
    }

    @Test
    @DisplayName("경로 변수 검증 실패를 공통 400 응답으로 변환한다")
    void handlesConstraintViolationException() throws Exception {
        mockMvc.perform(get("/api/admin/questions/{questionId}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("읽을 수 없는 JSON을 내부 정보가 없는 공통 400 응답으로 변환한다")
    void handlesUnreadableRequestBody() throws Exception {
        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다."));
    }

    @Test
    @DisplayName("숫자로 변환할 수 없는 경로 변수를 공통 400 응답으로 변환한다")
    void handlesTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/errors/type-mismatch").queryParam("value", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("value"));
    }

    @Test
    @DisplayName("도메인 입력 오류를 공통 400 응답으로 변환한다")
    void handlesIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/errors/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("잘못된 도메인 값입니다."));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 내부 정보를 숨긴 공통 500 응답으로 변환한다")
    void handlesUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버에서 요청을 처리하는 중 오류가 발생했습니다."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("database password"))));
    }

    @RestController
    static class FailureController {

        @GetMapping("/test/errors/type-mismatch")
        void typeMismatch(@RequestParam Long value) {
        }

        @GetMapping("/test/errors/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("잘못된 도메인 값입니다.");
        }

        @GetMapping("/test/errors/unexpected")
        void unexpected() {
            throw new RuntimeException("database password must not be exposed");
        }
    }
}
