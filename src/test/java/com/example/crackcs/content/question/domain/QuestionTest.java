package com.example.crackcs.content.question.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
                .createdByMember(createAdmin())
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
    @DisplayName("비활성 Concept은 문제에 연결할 수 없다")
    void rejectsInactiveConcept() {
        Topic topic = createTopic();
        Concept inactiveConcept = createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드");
        inactiveConcept.deactivate();
        Question question = createQuestion(topic, "질문", "모범 답안");

        assertThatThrownBy(() -> question.addConcept(inactiveConcept, BigDecimal.ONE, true))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("비활성 Concept은 문제에 연결할 수 없습니다.");
    }

    @Test
    @DisplayName("문제와 다른 Topic의 Concept은 연결할 수 없다")
    void rejectsConceptFromDifferentTopic() {
        Topic questionTopic = createTopic("OPERATING_SYSTEM", "운영체제");
        Concept concept = createConcept(
                createTopic("NETWORK", "네트워크"),
                "TCP",
                "TCP"
        );
        Question question = createQuestion(questionTopic, "질문", "모범 답안");

        assertThatThrownBy(() -> question.addConcept(concept, BigDecimal.ONE, true))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("문제와 같은 Topic의 Concept만 연결할 수 있습니다.");
    }

    @Test
    @DisplayName("Concept 교체 검증에 실패하면 기존 연결과 수정 시각을 유지한다")
    void failedConceptReplacementDoesNotChangeQuestion() {
        Topic topic = createTopic();
        Question question = createQuestion(topic, "질문", "모범 답안");
        QuestionConcept original = question.addConcept(
                createConcept(topic, "PROCESS", "프로세스"),
                BigDecimal.ONE,
                true
        );
        LocalDateTime updatedAt = question.getUpdatedAt();
        Concept validReplacement = createConcept(topic, "THREAD", "스레드");
        Concept inactiveReplacement = createConcept(topic, "SCHEDULER", "스케줄러");
        inactiveReplacement.deactivate();

        assertThatThrownBy(() -> question.replaceConcepts(List.of(
                new QuestionConceptAssignment(validReplacement, new BigDecimal("0.50"), true),
                new QuestionConceptAssignment(inactiveReplacement, new BigDecimal("0.50"), false)
        )))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("비활성 Concept은 문제에 연결할 수 없습니다.");

        assertThat(question.getQuestionConcepts()).containsExactly(original);
        assertThat(question.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Concept 교체 시 문제와 다른 Topic의 Concept은 연결할 수 없다")
    void replacementRejectsConceptFromDifferentTopic() {
        Topic questionTopic = createTopic("OPERATING_SYSTEM", "운영체제");
        Concept otherTopicConcept = createConcept(
                createTopic("NETWORK", "네트워크"),
                "TCP",
                "TCP"
        );
        Question question = createQuestion(questionTopic, "질문", "모범 답안");

        assertThatThrownBy(() -> question.replaceConcepts(List.of(
                new QuestionConceptAssignment(otherTopicConcept, BigDecimal.ONE, true)
        )))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("문제와 같은 Topic의 Concept만 연결할 수 있습니다.");
    }

    @Test
    @DisplayName("검수하지 않은 문제는 평가 Concept이 있어도 공개할 수 없다")
    void rejectsPublishWithoutReview() {
        Topic topic = createTopic();
        Question question = createQuestion(topic, "질문", "모범 답안");
        question.addConcept(createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드"), BigDecimal.ONE, true);

        assertThatThrownBy(question::publish)
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("검수자와 검수 시각이 있어야 문제를 공개할 수 있습니다.");
    }

    @Test
    @DisplayName("평가 개념 없이 문제를 공개할 수 없다")
    void rejectsPublishWithoutConcept() {
        Question question = createQuestion(createTopic(), "질문", "모범 답안");
        question.review(createAdmin());

        assertThatThrownBy(question::publish)
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("published question must have at least one concept");
    }

    @Test
    @DisplayName("평가 Concept 가중치 합이 1이 아니면 문제를 공개할 수 없다")
    void rejectsPublishWhenWeightsDoNotSumToOne() {
        Topic topic = createTopic();
        Question question = createQuestion(topic, "질문", "모범 답안");
        question.addConcept(createConcept(topic, "PROCESS", "프로세스"), new BigDecimal("0.60"), true);
        question.addConcept(createConcept(topic, "THREAD", "스레드"), new BigDecimal("0.30"), false);
        question.review(createAdmin());

        assertThatThrownBy(question::publish)
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("question concept weights must sum to 1.00");
    }

    @Test
    @DisplayName("PUBLISHED 문제는 직접 수정할 수 없다")
    void rejectsDirectUpdateOfPublishedQuestion() {
        Topic topic = createTopic();
        Question published = createQuestion(topic, "기존 질문", "기존 답안");
        published.addConcept(createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드"), BigDecimal.ONE, true);
        published.review(createAdmin());
        published.publish();

        assertThatThrownBy(() -> published.update(topic, QuestionDifficulty.ADVANCED, "변경 질문", "변경 답안"))
                .isInstanceOf(InvalidContentStateException.class);
    }

    @Test
    @DisplayName("PUBLISHED 문제에서 같은 계열의 다음 DRAFT 버전을 만든다")
    void createsNextVersionFromPublishedQuestion() {
        Topic topic = createTopic();
        Question published = createQuestion(topic, "기존 질문", "기존 답안");
        published.addConcept(createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드"), BigDecimal.ONE, true);
        published.review(createAdmin());
        published.publish();

        Question next = published.createNextVersion(
                2, createAdmin(), QuestionDifficulty.ADVANCED, "변경 질문", "변경 답안"
        );

        assertThat(published.getContent()).isEqualTo("기존 질문");
        assertThat(published.getStatus()).isEqualTo(QuestionStatus.PUBLISHED);
        assertThat(next.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(next.getQuestionVersion()).isEqualTo(2);
        assertThat(next.getVersionSeriesId()).isEqualTo(published.getVersionSeriesId());
        assertThat(next.getQuestionConcepts()).hasSize(1);
    }

    @Test
    @DisplayName("필수 평가 개념 없이 문제를 공개할 수 없다")
    void rejectsPublishWithoutRequiredConcept() {
        Topic topic = createTopic();
        Question question = createQuestion(topic, "질문", "모범 답안");
        question.addConcept(createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드"), BigDecimal.ONE, false);
        question.review(createAdmin());

        assertThatThrownBy(question::publish)
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("published question must have at least one required concept");
    }

    @Test
    @DisplayName("필수 평가 개념이 있는 문제를 공개한다")
    void publishesQuestionWithRequiredConcept() {
        Topic topic = createTopic();
        Question question = createQuestion(topic, "질문", "모범 답안");
        question.addConcept(createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드"), BigDecimal.ONE, true);
        question.review(createAdmin());

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
    @DisplayName("가중치 0은 허용하지 않는다")
    void rejectsZeroWeight() {
        Topic topic = createTopic();
        Concept concept = createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드");
        Question question = createQuestion(topic, "질문", "모범 답안");

        assertThatThrownBy(() -> question.addConcept(concept, BigDecimal.ZERO, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weight must be greater than 0 and less than or equal to 1");
    }

    @Test
    @DisplayName("가중치 상한 1은 허용한다")
    void acceptsOneAsWeight() {
        Topic topic = createTopic();
        Concept concept = createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드");
        Question question = createQuestion(topic, "질문", "모범 답안");

        QuestionConcept questionConcept = question.addConcept(concept, BigDecimal.ONE, true);

        assertThat(questionConcept.getWeight()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("가중치 1 초과는 허용하지 않는다")
    void rejectsWeightGreaterThanOne() {
        Topic topic = createTopic();
        Concept concept = createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드");
        Question question = createQuestion(topic, "질문", "모범 답안");

        assertThatThrownBy(() -> question.addConcept(concept, new BigDecimal("1.01"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weight must be greater than 0 and less than or equal to 1");
    }

    private Question createQuestion(Topic topic, String content, String referenceAnswer) {
        return Question.builder()
                .topic(topic)
                .createdByMember(createAdmin())
                .difficulty(QuestionDifficulty.BASIC)
                .content(content)
                .referenceAnswer(referenceAnswer)
                .build();
    }

    private Member createAdmin() {
        return Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
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
