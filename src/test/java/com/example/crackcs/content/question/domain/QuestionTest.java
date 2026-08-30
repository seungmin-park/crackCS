package com.example.crackcs.content.question.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.topic.domain.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTest {

    @Test
    @DisplayName("빌더로 관리자 초안 일반 문제를 생성한다")
    void builderCreatesNormalQuestionAsAdminDraft() {
        Topic topic = createTopic("OPERATING_SYSTEM", "운영체제");

        Question question = createQuestion(topic, "프로세스와 스레드의 차이를 설명하세요.", "모범 답안");

        assertThat(question.getId()).isNull();
        assertThat(question.getTopic()).isSameAs(topic);
        assertThat(question.getOrigin()).isEqualTo(QuestionOrigin.ADMIN);
        assertThat(question.getType()).isEqualTo(QuestionType.NORMAL);
        assertThat(question.getDifficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(question.getQuestionConcepts()).isEmpty();
    }

    @Test
    @DisplayName("문제를 생성하면 생성 시각과 수정 시각을 자동으로 등록한다")
    void builderAutomaticallyRegistersCreatedAtAndUpdatedAt() {
        LocalDateTime beforeCreation = LocalDateTime.now();

        Question question = createQuestion(createTopic(), "질문", "모범 답안");

        LocalDateTime afterCreation = LocalDateTime.now();
        assertThat(question.getCreatedAt()).isBetween(beforeCreation, afterCreation);
        assertThat(question.getUpdatedAt()).isEqualTo(question.getCreatedAt());
    }

    @Test
    @DisplayName("update 메서드로 문제 정보를 수정한다")
    void questionCanBeUpdatedThroughDomainMethod() {
        Topic originalTopic = createTopic("OPERATING_SYSTEM", "운영체제");
        Topic updatedTopic = createTopic("NETWORK", "네트워크");
        Question question = createQuestion(originalTopic, "기존 질문", "기존 모범 답안");
        LocalDateTime createdAt = question.getCreatedAt();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        question.update(updatedTopic, QuestionDifficulty.ADVANCED, "변경된 질문", "변경된 모범 답안");

        LocalDateTime afterUpdate = LocalDateTime.now();
        assertThat(question.getTopic()).isSameAs(updatedTopic);
        assertThat(question.getDifficulty()).isEqualTo(QuestionDifficulty.ADVANCED);
        assertThat(question.getContent()).isEqualTo("변경된 질문");
        assertThat(question.getReferenceAnswer()).isEqualTo("변경된 모범 답안");
        assertThat(question.getCreatedAt()).isEqualTo(createdAt);
        assertThat(question.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
    }

    @Test
    @DisplayName("수정 값 검증에 실패하면 기존 정보를 변경하지 않는다")
    void failedUpdateDoesNotChangeAnyField() {
        Topic originalTopic = createTopic("OPERATING_SYSTEM", "운영체제");
        Question question = createQuestion(originalTopic, "기존 질문", "기존 모범 답안");
        LocalDateTime updatedAt = question.getUpdatedAt();

        assertThatThrownBy(() -> question.update(
                createTopic("NETWORK", "네트워크"),
                QuestionDifficulty.ADVANCED,
                "   ",
                "변경된 모범 답안"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be blank");

        assertThat(question.getTopic()).isSameAs(originalTopic);
        assertThat(question.getDifficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(question.getContent()).isEqualTo("기존 질문");
        assertThat(question.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("주제는 필수이다")
    void topicMustNotBeNull() {
        assertThatThrownBy(() -> createQuestion(null, "질문", "모범 답안"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic must not be null");
    }

    @Test
    @DisplayName("난이도는 필수이다")
    void difficultyMustNotBeNull() {
        assertThatThrownBy(() -> Question.builder()
                .topic(createTopic())
                .content("질문")
                .referenceAnswer("모범 답안")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("difficulty must not be null");
    }

    @Test
    @DisplayName("문제 본문은 공백일 수 없다")
    void contentMustNotBeBlank() {
        assertThatThrownBy(() -> createQuestion(createTopic(), "   ", "모범 답안"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be blank");
    }

    @Test
    @DisplayName("모범 답안은 공백일 수 없다")
    void referenceAnswerMustNotBeBlank() {
        assertThatThrownBy(() -> createQuestion(createTopic(), "질문", "\n\t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("referenceAnswer must not be blank");
    }

    @Test
    @DisplayName("문제에 평가 개념과 가중치 및 필수 여부를 연결한다")
    void addsQuestionConcept() {
        Topic topic = createTopic();
        Concept concept = createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드");
        Question question = createQuestion(topic, "질문", "모범 답안");

        QuestionConcept questionConcept = question.addConcept(concept, new BigDecimal("0.70"), true);

        assertThat(questionConcept.getQuestion()).isSameAs(question);
        assertThat(questionConcept.getConcept()).isSameAs(concept);
        assertThat(questionConcept.getWeight()).isEqualByComparingTo("0.70");
        assertThat(questionConcept.isRequired()).isTrue();
    }

    @Test
    @DisplayName("평가 개념 없이 문제를 공개할 수 없다")
    void rejectsPublishWithoutConcept() {
        Question question = createQuestion(createTopic(), "질문", "모범 답안");

        assertThatThrownBy(question::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("published question must have at least one concept");
    }

    @Test
    @DisplayName("필수 평가 개념 없이 문제를 공개할 수 없다")
    void rejectsPublishWithoutRequiredConcept() {
        Topic topic = createTopic();
        Question question = createQuestion(topic, "질문", "모범 답안");
        question.addConcept(createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드"), BigDecimal.ONE, false);

        assertThatThrownBy(question::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("published question must have at least one required concept");
    }

    @Test
    @DisplayName("필수 평가 개념이 있는 문제를 공개한다")
    void publishesQuestionWithRequiredConcept() {
        Topic topic = createTopic();
        Question question = createQuestion(topic, "질문", "모범 답안");
        question.addConcept(createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드"), BigDecimal.ONE, true);

        question.publish();

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PUBLISHED);
    }

    @Test
    @DisplayName("같은 평가 개념을 중복 연결할 수 없다")
    void rejectsDuplicatedConcept() {
        Topic topic = createTopic();
        Concept concept = createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드");
        Question question = createQuestion(topic, "질문", "모범 답안");
        question.addConcept(concept, BigDecimal.ONE, true);

        assertThatThrownBy(() -> question.addConcept(concept, new BigDecimal("0.50"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("concept must not be duplicated");
    }

    @Test
    @DisplayName("가중치는 0보다 크고 1 이하여야 한다")
    void validatesWeightRange() {
        Topic topic = createTopic();
        Concept concept = createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드");
        Question question = createQuestion(topic, "질문", "모범 답안");

        assertThatThrownBy(() -> question.addConcept(concept, BigDecimal.ZERO, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weight must be greater than 0 and less than or equal to 1");
        assertThatThrownBy(() -> question.addConcept(concept, new BigDecimal("1.01"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weight must be greater than 0 and less than or equal to 1");
    }

    private Question createQuestion(Topic topic, String content, String referenceAnswer) {
        return Question.builder()
                .topic(topic)
                .difficulty(QuestionDifficulty.BASIC)
                .content(content)
                .referenceAnswer(referenceAnswer)
                .build();
    }

    private Topic createTopic() {
        return createTopic("OPERATING_SYSTEM", "운영체제");
    }

    private Topic createTopic(String code, String name) {
        return Topic.builder()
                .code(code)
                .name(name)
                .build();
    }

    private Concept createConcept(Topic topic, String code, String name) {
        return Concept.builder()
                .topic(topic)
                .code(code)
                .name(name)
                .build();
    }
}
