package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.*;
import com.example.crackcs.content.question.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("문제 서비스")
class QuestionServiceTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionRepository questionRepository;

    @BeforeEach
    void setup() {
        questionRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("관리자 초안 일반 문제를 생성하고 저장한다")
    void createsAndSavesQuestion() {
        Question createdQuestion = questionService.create(
                1L,
                QuestionDifficulty.BASIC,
                "프로세스와 스레드의 차이를 설명하세요.",
                "프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다."
        );

        Question savedQuestion = questionRepository.findById(createdQuestion.getId()).orElseThrow();

        assertThat(savedQuestion.getTopicId()).isEqualTo(1L);
        assertThat(savedQuestion.getOrigin()).isEqualTo(QuestionOrigin.ADMIN);
        assertThat(savedQuestion.getType()).isEqualTo(QuestionType.NORMAL);
        assertThat(savedQuestion.getDifficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(savedQuestion.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(savedQuestion.getContent()).isEqualTo("프로세스와 스레드의 차이를 설명하세요.");
        assertThat(savedQuestion.getReferenceAnswer())
                .isEqualTo("프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다.");
    }

    @Test
    @DisplayName("저장된 문제 목록을 조회한다")
    void findsAllQuestions() {
        saveQuestion("첫 번째 질문");
        saveQuestion("두 번째 질문");

        List<Question> questions = questionService.findAll();

        assertThat(questions)
                .extracting(Question::getContent)
                .containsExactlyInAnyOrder("첫 번째 질문", "두 번째 질문");
    }

    @Test
    @DisplayName("ID에 해당하는 문제를 조회한다")
    void findsQuestionById() {
        Question savedQuestion = saveQuestion("기존 질문");
        Long questionId = savedQuestion.getId();

        Question foundQuestion = questionService.findById(questionId);

        assertThat(foundQuestion.getId()).isEqualTo(questionId);
        assertThat(foundQuestion.getContent()).isEqualTo("기존 질문");
    }

    @Test
    @DisplayName("ID에 해당하는 문제가 없으면 예외를 던진다")
    void throwsExceptionWhenQuestionDoesNotExist() {
        assertThatThrownBy(() -> questionService.findById(Long.MAX_VALUE))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Question not found: " + Long.MAX_VALUE);
    }

    @Test
    @DisplayName("문제를 조회한 뒤 정보를 수정하고 저장한다")
    void updatesAndSavesQuestion() {
        Question savedQuestion = saveQuestion("기존 질문");
        Long questionId = savedQuestion.getId();
        LocalDateTime createdAt = savedQuestion.getCreatedAt();
        LocalDateTime firstUpdatedAt = savedQuestion.getUpdatedAt();

        questionService.update(
                questionId,
                2L,
                QuestionDifficulty.ADVANCED,
                "변경된 질문",
                "변경된 모범 답안"
        );

        Question updatedQuestion = questionService.findById(questionId);

        assertThat(updatedQuestion.getTopicId()).isEqualTo(2L);
        assertThat(updatedQuestion.getDifficulty()).isEqualTo(QuestionDifficulty.ADVANCED);
        assertThat(updatedQuestion.getContent()).isEqualTo("변경된 질문");
        assertThat(updatedQuestion.getReferenceAnswer()).isEqualTo("변경된 모범 답안");
        assertThat(updatedQuestion.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updatedQuestion.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
    }

    @Test
    @DisplayName("수정할 문제가 없으면 예외를 던진다")
    void throwsExceptionWhenQuestionToUpdateDoesNotExist() {
        assertThatThrownBy(() -> questionService.update(
                Long.MAX_VALUE,
                2L,
                QuestionDifficulty.ADVANCED,
                "변경된 질문",
                "변경된 모범 답안"
        ))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Question not found: " + Long.MAX_VALUE);
    }

    private Question saveQuestion(String content) {
        Question question = Question.builder()
                .topicId(1L)
                .difficulty(QuestionDifficulty.BASIC)
                .content(content)
                .referenceAnswer("모범 답안")
                .build();

        return questionRepository.save(question);
    }
}
