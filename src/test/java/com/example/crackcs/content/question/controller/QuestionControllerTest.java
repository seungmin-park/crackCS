package com.example.crackcs.content.question.controller;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("문제 API")
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @BeforeEach
    void setup() {
        questionRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("관리자 초안 문제를 생성하면 201과 생성된 문제를 반환한다")
    void createsQuestion() throws Exception {
        Map<String, Object> request = Map.of(
                "topicId", 1L,
                "difficulty", "BASIC",
                "content", "프로세스와 스레드의 차이를 설명하세요.",
                "referenceAnswer", "프로세스는 자원을 독립적으로 소유하고 스레드는 자원을 공유합니다."
        );

        mockMvc.perform(post("/api/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/admin/questions/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.topicId").value(1))
                .andExpect(jsonPath("$.origin").value("ADMIN"))
                .andExpect(jsonPath("$.type").value("NORMAL"))
                .andExpect(jsonPath("$.difficulty").value("BASIC"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertThat(questionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("조건과 페이지를 적용해 문제 목록을 조회한다")
    void findsQuestionsWithFiltersAndPaging() throws Exception {
        saveQuestion(1L, QuestionDifficulty.BASIC, "첫 번째 질문");
        saveQuestion(1L, QuestionDifficulty.ADVANCED, "두 번째 질문");
        saveQuestion(2L, QuestionDifficulty.BASIC, "다른 Topic 질문");

        mockMvc.perform(get("/api/admin/questions")
                        .param("topicId", "1")
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
    }

    @Test
    @DisplayName("ID로 문제 상세를 조회한다")
    void findsQuestionById() throws Exception {
        Question question = saveQuestion(1L, QuestionDifficulty.INTERMEDIATE, "상세 질문");

        mockMvc.perform(get("/api/admin/questions/{questionId}", question.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(question.getId()))
                .andExpect(jsonPath("$.difficulty").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.content").value("상세 질문"))
                .andExpect(jsonPath("$.referenceAnswer").value("모범 답안"));
    }

    @Test
    @DisplayName("문제 정보를 수정하면 변경된 응답과 저장 상태를 반환한다")
    void updatesQuestion() throws Exception {
        Question question = saveQuestion(1L, QuestionDifficulty.BASIC, "수정 전 질문");
        Map<String, Object> request = Map.of(
                "topicId", 2L,
                "difficulty", "ADVANCED",
                "content", "수정된 질문",
                "referenceAnswer", "수정된 모범 답안"
        );

        mockMvc.perform(patch("/api/admin/questions/{questionId}", question.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(2))
                .andExpect(jsonPath("$.difficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.content").value("수정된 질문"))
                .andExpect(jsonPath("$.referenceAnswer").value("수정된 모범 답안"));

        Question updatedQuestion = questionRepository.findById(question.getId()).orElseThrow();
        assertThat(updatedQuestion.getContent()).isEqualTo("수정된 질문");
    }

    @Test
    @DisplayName("존재하지 않는 문제를 조회하면 공통 404 오류를 반환한다")
    void returnsNotFoundError() throws Exception {
        mockMvc.perform(get("/api/admin/questions/{questionId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Question not found: " + Long.MAX_VALUE))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("문제 생성 값이 올바르지 않으면 필드 오류와 400을 반환한다")
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

        assertThat(questionRepository.count()).isZero();
    }

    @Test
    @DisplayName("지원하지 않는 목록 조건은 공통 400 오류를 반환한다")
    void returnsInvalidQueryError() throws Exception {
        mockMvc.perform(get("/api/admin/questions")
                        .param("difficulty", "EXPERT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("difficulty"))
                .andExpect(jsonPath("$.fieldErrors[0].reason")
                        .value("difficulty는 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
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
    }

    @Test
    @DisplayName("문제 ID가 양수가 아니면 공통 400 오류를 반환한다")
    void returnsInvalidQuestionIdError() throws Exception {
        mockMvc.perform(get("/api/admin/questions/{questionId}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].reason").value("questionId는 양수여야 합니다."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("지원하지 않는 정렬 필드는 공통 400 오류를 반환한다")
    void returnsInvalidSortError() throws Exception {
        mockMvc.perform(get("/api/admin/questions")
                        .param("sort", "content,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 정렬 필드입니다: content"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    private Question saveQuestion(Long topicId, QuestionDifficulty difficulty, String content) {
        return questionRepository.save(Question.builder()
                .topicId(topicId)
                .difficulty(difficulty)
                .content(content)
                .referenceAnswer("모범 답안")
                .build());
    }
}
