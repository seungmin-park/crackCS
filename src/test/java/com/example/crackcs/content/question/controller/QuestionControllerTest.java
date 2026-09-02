package com.example.crackcs.content.question.controller;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.content.question.service.QuestionService;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.auth.security.AuthenticatedMember;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuestionService questionService;

    @BeforeEach
    void authenticateAdmin() {
        AuthenticatedMember principal = adminPrincipal(77L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, principal.getPassword(), principal.getAuthorities()
                )
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("관리자 초안 문제를 생성하면 201과 생성된 문제를 반환한다")
    void createsQuestion() throws Exception {
        Topic firstTopic = topic(1L, "OPERATING_SYSTEM", "운영체제");
        Question created = question(10L, firstTopic, QuestionDifficulty.BASIC,
                "프로세스와 스레드의 차이를 설명하세요.",
                "프로세스는 자원을 독립적으로 소유하고 스레드는 자원을 공유합니다.");
        given(questionService.create(
                77L,
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
                        .principal(SecurityContextHolder.getContext().getAuthentication())
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
                77L,
                firstTopic.getId(),
                QuestionDifficulty.BASIC,
                created.getContent(),
                created.getReferenceAnswer()
        );
    }

    @Test
    @DisplayName("목록 조건을 Service에 전달하고 문제 목록 응답을 반환한다")
    void findsQuestionsWithFilters() throws Exception {
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
    @DisplayName("page 하한 0과 size 하한 1을 허용한다")
    void acceptsPagingLowerBounds() throws Exception {
        given(questionService.findAll(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        mockMvc.perform(get("/api/admin/questions").param("page", "0").param("size", "1"))
                .andExpect(status().isOk());

        verify(questionService).findAll(
                isNull(), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 1)
        );
    }

    @Test
    @DisplayName("size 상한 100을 허용한다")
    void acceptsSizeUpperBound() throws Exception {
        given(questionService.findAll(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        mockMvc.perform(get("/api/admin/questions").param("size", "100"))
                .andExpect(status().isOk());

        verify(questionService).findAll(
                isNull(), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    @DisplayName("page 하한보다 작은 -1은 거부한다")
    void rejectsPageBelowLowerBound() throws Exception {
        mockMvc.perform(get("/api/admin/questions").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"))
                .andExpect(jsonPath("$.fieldErrors[0].reason").value("page는 0 이상이어야 합니다."));

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("size 상한보다 큰 101은 거부한다")
    void rejectsSizeAboveUpperBound() throws Exception {
        mockMvc.perform(get("/api/admin/questions").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"))
                .andExpect(jsonPath("$.fieldErrors[0].reason").value("size는 100 이하여야 합니다."));

        verifyNoInteractions(questionService);
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
    @DisplayName("문제 생성 topicId가 양수가 아니면 Service를 호출하지 않고 400을 반환한다")
    void rejectsNonPositiveTopicId() throws Exception {
        Map<String, Object> request = Map.of(
                "topicId", 0L,
                "difficulty", "BASIC",
                "content", "문제 본문",
                "referenceAnswer", "모범 답안"
        );

        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("topicId"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("문제 본문이 공백이면 Service를 호출하지 않고 400을 반환한다")
    void rejectsBlankContent() throws Exception {
        Map<String, Object> request = Map.of(
                "topicId", 1L,
                "difficulty", "BASIC",
                "content", " ",
                "referenceAnswer", "모범 답안"
        );

        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"))
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
        Topic topic = mock(Topic.class);
        given(topic.getId()).willReturn(id);
        given(topic.getCode()).willReturn(code);
        given(topic.getName()).willReturn(name);
        return topic;
    }

    private Question question(
            Long id,
            Topic topic,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        Long topicId = topic.getId();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        Question question = mock(Question.class);
        given(question.getId()).willReturn(id);
        given(question.getTopicId()).willReturn(topicId);
        given(question.getOrigin()).willReturn(QuestionOrigin.ADMIN);
        given(question.getType()).willReturn(QuestionType.NORMAL);
        given(question.getDifficulty()).willReturn(difficulty);
        given(question.getContent()).willReturn(content);
        given(question.getReferenceAnswer()).willReturn(referenceAnswer);
        given(question.getStatus()).willReturn(QuestionStatus.DRAFT);
        given(question.getCreatedAt()).willReturn(createdAt);
        given(question.getUpdatedAt()).willReturn(createdAt);
        return question;
    }

    private AuthenticatedMember adminPrincipal(Long memberId) {
        AuthenticatedMember principal = mock(AuthenticatedMember.class);
        given(principal.memberId()).willReturn(memberId);
        given(principal.getUsername()).willReturn("admin@example.com");
        given(principal.getPassword()).willReturn("encoded");
        org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(principal).getAuthorities();
        given(principal.isEnabled()).willReturn(true);
        return principal;
    }
}
