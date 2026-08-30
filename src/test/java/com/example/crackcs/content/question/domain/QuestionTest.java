package com.example.crackcs.content.question.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question 도메인")
class QuestionTest {

    @Test
    @DisplayName("빌더로 관리자 초안 일반 문제를 생성한다")
    void builderCreatesNormalQuestionAsAdminDraft() {
        Question question = Question.builder()
                .topicId(1L)
                .difficulty(QuestionDifficulty.BASIC)
                .content("프로세스와 스레드의 차이를 설명하세요.")
                .referenceAnswer("프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다.")
                .build();

        assertThat(question.getId()).isNull();
        assertThat(question.getTopicId()).isEqualTo(1L);
        assertThat(question.getOrigin()).isEqualTo(QuestionOrigin.ADMIN);
        assertThat(question.getType()).isEqualTo(QuestionType.NORMAL);
        assertThat(question.getDifficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(question.getContent()).isEqualTo("프로세스와 스레드의 차이를 설명하세요.");
        assertThat(question.getReferenceAnswer())
                .isEqualTo("프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다.");
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.DRAFT);
    }

    @Test
    @DisplayName("문제를 생성하면 생성 시각과 수정 시각을 자동으로 등록한다")
    void builderAutomaticallyRegistersCreatedAtAndUpdatedAt() {
        LocalDateTime beforeCreation = LocalDateTime.now();

        Question question = createQuestion(1L, "질문", "모범 답안");

        LocalDateTime afterCreation = LocalDateTime.now();

        assertThat(question.getCreatedAt()).isBetween(beforeCreation, afterCreation);
        assertThat(question.getUpdatedAt()).isEqualTo(question.getCreatedAt());
    }

    @Test
    @DisplayName("update 메서드로 문제 정보를 수정한다")
    void questionCanBeUpdatedThroughDomainMethod() {
        Question question = createQuestion(1L, "기존 질문", "기존 모범 답안");
        LocalDateTime createdAt = question.getCreatedAt();
        LocalDateTime firstUpdatedAt = question.getUpdatedAt();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        question.update(
                2L,
                QuestionDifficulty.ADVANCED,
                "변경된 질문",
                "변경된 모범 답안"
        );

        LocalDateTime afterUpdate = LocalDateTime.now();

        assertThat(question.getTopicId()).isEqualTo(2L);
        assertThat(question.getDifficulty()).isEqualTo(QuestionDifficulty.ADVANCED);
        assertThat(question.getContent()).isEqualTo("변경된 질문");
        assertThat(question.getReferenceAnswer()).isEqualTo("변경된 모범 답안");
        assertThat(question.getCreatedAt()).isEqualTo(createdAt);
        assertThat(question.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
        assertThat(question.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
    }

    @Test
    @DisplayName("수정 값 검증에 실패하면 기존 정보를 변경하지 않는다")
    void failedUpdateDoesNotChangeAnyField() {
        Question question = createQuestion(1L, "기존 질문", "기존 모범 답안");
        LocalDateTime updatedAt = question.getUpdatedAt();

        assertThatThrownBy(() -> question.update(
                2L,
                QuestionDifficulty.ADVANCED,
                "   ",
                "변경된 모범 답안"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be blank");

        assertThat(question.getTopicId()).isEqualTo(1L);
        assertThat(question.getDifficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(question.getContent()).isEqualTo("기존 질문");
        assertThat(question.getReferenceAnswer()).isEqualTo("기존 모범 답안");
        assertThat(question.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Topic ID는 양수여야 한다")
    void topicIdMustBePositive() {
        assertThatThrownBy(() -> createQuestion(0L, "질문", "모범 답안"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topicId must be positive");
    }

    @Test
    @DisplayName("난이도는 필수이다")
    void difficultyMustNotBeNull() {
        assertThatThrownBy(() -> Question.builder()
                .topicId(1L)
                .content("질문")
                .referenceAnswer("모범 답안")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("difficulty must not be null");
    }

    @Test
    @DisplayName("문제 본문은 공백일 수 없다")
    void contentMustNotBeBlank() {
        assertThatThrownBy(() -> createQuestion(1L, "   ", "모범 답안"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be blank");
    }

    @Test
    @DisplayName("모범 답안은 공백일 수 없다")
    void referenceAnswerMustNotBeBlank() {
        assertThatThrownBy(() -> createQuestion(1L, "질문", "\n\t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("referenceAnswer must not be blank");
    }

    private Question createQuestion(Long topicId, String content, String referenceAnswer) {
        return Question.builder()
                .topicId(topicId)
                .difficulty(QuestionDifficulty.BASIC)
                .content(content)
                .referenceAnswer(referenceAnswer)
                .build();
    }
}
