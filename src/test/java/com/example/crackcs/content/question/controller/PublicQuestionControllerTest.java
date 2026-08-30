package com.example.crackcs.content.question.controller;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.service.PublicQuestionService;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.content.topic.domain.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicQuestionController.class)
class PublicQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicQuestionService publicQuestionService;

    @Test
    @DisplayName("조회 조건을 Service에 전달하고 공개 문제 페이지를 반환한다")
    void findsPublishedQuestionPage() throws Exception {
        Topic topic = createTopic();
        Question publicQuestion = createPublicQuestion(topic);
        given(publicQuestionService.findAll(
                eq(topic.getId()),
                eq(QuestionDifficulty.BASIC),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(publicQuestion), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/questions")
                        .param("topicId", topic.getId().toString())
                        .param("difficulty", "BASIC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("공개 질문"))
                .andExpect(jsonPath("$.content[0].topic.name").value("운영체제"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(publicQuestionService).findAll(
                eq(topic.getId()),
                eq(QuestionDifficulty.BASIC),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("공개 문제 상세 응답에는 모범 답안과 평가 정보가 없다")
    void hidesEvaluationFieldsFromDetail() throws Exception {
        Topic topic = createTopic();
        Question publicQuestion = createPublicQuestion(topic);
        given(publicQuestionService.findById(publicQuestion.getId())).willReturn(publicQuestion);

        mockMvc.perform(get("/api/questions/{questionId}", publicQuestion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicQuestion.getId()))
                .andExpect(jsonPath("$.topic.code").value("OPERATING_SYSTEM"))
                .andExpect(jsonPath("$.difficulty").value("BASIC"))
                .andExpect(jsonPath("$.content").value("공개 질문"))
                .andExpect(jsonPath("$", not(hasKey("referenceAnswer"))))
                .andExpect(jsonPath("$", not(hasKey("questionConcepts"))))
                .andExpect(jsonPath("$", not(hasKey("weight"))));

        verify(publicQuestionService).findById(publicQuestion.getId());
    }

    @Test
    @DisplayName("Service가 비공개 문제와 폐기 문제를 숨기면 모두 같은 404를 반환한다")
    void hidesDraftAndRetiredDetails() throws Exception {
        given(publicQuestionService.findById(20L)).willThrow(new QuestionNotFoundException(20L));
        given(publicQuestionService.findById(21L)).willThrow(new QuestionNotFoundException(21L));

        mockMvc.perform(get("/api/questions/{questionId}", 20L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
        mockMvc.perform(get("/api/questions/{questionId}", 21L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("Service의 문제 없음 예외를 공개 API의 404로 반환한다")
    void returnsNotFoundForUnknownQuestion() throws Exception {
        given(publicQuestionService.findById(Long.MAX_VALUE))
                .willThrow(new QuestionNotFoundException(Long.MAX_VALUE));

        mockMvc.perform(get("/api/questions/{questionId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
    }

    private Topic createTopic() {
        Topic topic = Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build();
        ReflectionTestUtils.setField(topic, "id", 1L);
        return topic;
    }

    private Question createPublicQuestion(Topic topic) {
        Question question = Question.builder()
                .topic(topic)
                .difficulty(QuestionDifficulty.BASIC)
                .content("공개 질문")
                .referenceAnswer("외부에 노출하면 안 되는 모범 답안")
                .build();
        ReflectionTestUtils.setField(question, "id", 10L);
        return question;
    }
}
