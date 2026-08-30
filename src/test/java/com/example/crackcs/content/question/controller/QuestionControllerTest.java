package com.example.crackcs.content.question.controller;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.content.question.service.QuestionService;
import com.example.crackcs.content.topic.domain.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuestionService questionService;

    @Test
    @DisplayName("관리자 초안 문제를 생성하면 201과 생성된 문제를 반환한다")
    void createsQuestion() throws Exception {
        Topic firstTopic = topic(1L, "OPERATING_SYSTEM", "운영체제");
        Question created = question(10L, firstTopic, QuestionDifficulty.BASIC,
                "프로세스와 스레드의 차이를 설명하세요.",
                "프로세스는 자원을 독립적으로 소유하고 스레드는 자원을 공유합니다.");
        given(questionService.create(
                firstTopic.getId(),
                QuestionDifficulty.BASIC,
                created.getContent(),
                created.getReferenceAnswer()
        )).willReturn(created);
        Map<String, Object> request = Map.of(
                "topicId", firstTopic.getId(),
                "difficulty", "BASIC",
                "content", created.getContent(),
                "referenceAnswer", created.getReferenceAnswer()
        );

        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/questions/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.topicId").value(firstTopic.getId()))
                .andExpect(jsonPath("$.origin").value("ADMIN"))
                .andExpect(jsonPath("$.type").value("NORMAL"))
                .andExpect(jsonPath("$.difficulty").value("BASIC"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        verify(questionService).create(
                firstTopic.getId(),
                QuestionDifficulty.BASIC,
                created.getContent(),
                created.getReferenceAnswer()
        );
    }

    @Test
    @DisplayName("조건과 페이지를 Service에 전달하고 문제 목록 응답을 반환한다")
    void findsQuestionsWithFiltersAndPaging() throws Exception {
        Topic firstTopic = topic(1L, "OPERATING_SYSTEM", "운영체제");
        Question question = question(11L, firstTopic, QuestionDifficulty.BASIC, "첫 번째 질문", "모범 답안");
        given(questionService.findAll(
                eq(firstTopic.getId()),
                isNull(),
                eq(QuestionDifficulty.BASIC),
                isNull(),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(question), PageRequest.of(0, 1), 1));

        mockMvc.perform(get("/api/admin/questions")
                        .param("topicId", firstTopic.getId().toString())
                        .param("difficulty", "BASIC")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("첫 번째 질문"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(questionService).findAll(
                eq(firstTopic.getId()),
                isNull(),
                eq(QuestionDifficulty.BASIC),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("ID를 Service에 전달하고 문제 상세를 반환한다")
    void findsQuestionById() throws Exception {
        Topic firstTopic = topic(1L, "OPERATING_SYSTEM", "운영체제");
        Question question = question(12L, firstTopic, QuestionDifficulty.INTERMEDIATE, "상세 질문", "모범 답안");
        given(questionService.findById(question.getId())).willReturn(question);

        mockMvc.perform(get("/api/admin/questions/{questionId}", question.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(question.getId()))
                .andExpect(jsonPath("$.difficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.content").value("상세 질문"))
                .andExpect(jsonPath("$.referenceAnswer").value("모범 답안"));

        verify(questionService).findById(question.getId());
    }

    @Test
    @DisplayName("수정 요청을 Service에 전달하고 변경된 문제 응답을 반환한다")
    void updatesQuestion() throws Exception {
        Topic secondTopic = topic(2L, "NETWORK", "네트워크");
        Question updated = question(13L, secondTopic, QuestionDifficulty.ADVANCED, "수정된 질문", "수정된 모범 답안");
        given(questionService.update(
                updated.getId(),
                secondTopic.getId(),
                QuestionDifficulty.ADVANCED,
                updated.getContent(),
                updated.getReferenceAnswer()
        )).willReturn(updated);
        Map<String, Object> request = Map.of(
                "topicId", secondTopic.getId(),
                "difficulty", "ADVANCED",
                "content", updated.getContent(),
                "referenceAnswer", updated.getReferenceAnswer()
        );

        mockMvc.perform(patch("/api/admin/questions/{questionId}", updated.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(secondTopic.getId()))
                .andExpect(jsonPath("$.difficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.content").value("수정된 질문"))
                .andExpect(jsonPath("$.referenceAnswer").value("수정된 모범 답안"));

        verify(questionService).update(
                updated.getId(),
                secondTopic.getId(),
                QuestionDifficulty.ADVANCED,
                updated.getContent(),
                updated.getReferenceAnswer()
        );
    }

    @Test
    @DisplayName("Service의 문제 없음 예외를 공통 404 오류로 반환한다")
    void returnsNotFoundError() throws Exception {
        given(questionService.findById(Long.MAX_VALUE))
                .willThrow(new QuestionNotFoundException(Long.MAX_VALUE));

        mockMvc.perform(get("/api/admin/questions/{questionId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Question not found: " + Long.MAX_VALUE))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("문제 생성 값이 올바르지 않으면 Service를 호출하지 않고 400을 반환한다")
    void returnsValidationError() throws Exception {
        Map<String, Object> request = Map.of(
                "topicId", 0L,
                "difficulty", "BASIC",
                "content", " ",
                "referenceAnswer", "모범 답안"
        );

        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("지원하지 않는 목록 조건은 Service를 호출하지 않고 공통 400 오류를 반환한다")
    void returnsInvalidQueryError() throws Exception {
        mockMvc.perform(get("/api/admin/questions").param("difficulty", "EXPERT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("difficulty"))
                .andExpect(jsonPath("$.fieldErrors[0].reason")
                        .value("difficulty는 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다."));

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("문제 생성 난이도가 허용값이 아니면 DTO의 검증 메시지를 반환한다")
    void returnsDifficultyValidationMessageFromRequestDto() throws Exception {
        Map<String, Object> request = Map.of(
                "topicId", 1L,
                "difficulty", "EXPERT",
                "content", "문제 본문",
                "referenceAnswer", "모범 답안"
        );

        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("difficulty"))
                .andExpect(jsonPath("$.fieldErrors[0].reason")
                        .value("difficulty는 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다."));

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("문제 ID가 양수가 아니면 Service를 호출하지 않고 공통 400 오류를 반환한다")
    void returnsInvalidQuestionIdError() throws Exception {
        mockMvc.perform(get("/api/admin/questions/{questionId}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].reason").value("questionId는 양수여야 합니다."));

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("지원하지 않는 정렬 필드는 Service를 호출하지 않고 공통 400 오류를 반환한다")
    void returnsInvalidSortError() throws Exception {
        mockMvc.perform(get("/api/admin/questions").param("sort", "content,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 정렬 필드입니다: content"));

        verifyNoInteractions(questionService);
    }

    private Topic topic(Long id, String code, String name) {
        Topic topic = Topic.builder().code(code).name(name).build();
        ReflectionTestUtils.setField(topic, "id", id);
        return topic;
    }

    private Question question(
            Long id,
            Topic topic,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        Question question = Question.builder()
                .topic(topic)
                .difficulty(difficulty)
                .content(content)
                .referenceAnswer(referenceAnswer)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
