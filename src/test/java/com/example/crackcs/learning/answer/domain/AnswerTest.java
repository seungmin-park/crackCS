package com.example.crackcs.learning.answer.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerTest {

    @Test
    @DisplayName("공개된 문제의 답변 원문과 멱등성 키를 변경 없이 보존한다")
    void createsImmutableSubmissionForPublishedQuestion() {
        Member learner = user("학습자");
        Question question = publishedQuestion();
        String idempotencyKey = UUID.randomUUID().toString();
        String content = "  공백도 답변 원문의 일부입니다.  ";

        Answer answer = Answer.builder().member(learner).question(question)
                .idempotencyKey(idempotencyKey).content(content).build();

        assertThat(answer.getMember()).isSameAs(learner);
        assertThat(answer.getQuestion()).isSameAs(question);
        assertThat(answer.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(answer.getContent()).isEqualTo(content);
        assertThat(answer.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("초안 문제에는 답변을 제출할 수 없다")
    void rejectsDraftQuestion() {
        Question draft = draftQuestion();

        assertThatThrownBy(() -> Answer.builder().member(user("학습자")).question(draft)
                .idempotencyKey(UUID.randomUUID().toString()).content("답변").build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("멱등성 키는 정규 UUID 형식이어야 한다")
    void rejectsNonCanonicalIdempotencyKey() {
        assertThatThrownBy(() -> Answer.builder().member(user("학습자")).question(publishedQuestion())
                .idempotencyKey("NOT-A-UUID").content("답변").build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("공백 답변과 10000자를 넘는 답변을 거부한다")
    void rejectsInvalidContent() {
        Question question = publishedQuestion();
        Member learner = user("학습자");
        String idempotencyKey = UUID.randomUUID().toString();

        assertThatThrownBy(() -> Answer.builder().member(learner).question(question)
                .idempotencyKey(idempotencyKey).content("   ").build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Answer.builder().member(learner).question(question)
                .idempotencyKey(idempotencyKey).content("가".repeat(10_001)).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("관리자 계정은 학습자 답변을 생성할 수 없다")
    void rejectsAdminSubmission() {
        assertThatThrownBy(() -> Answer.builder().member(admin("관리자")).question(publishedQuestion())
                .idempotencyKey(UUID.randomUUID().toString()).content("답변").build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("비활성 회원은 도메인 생성에서도 답변을 제출할 수 없다")
    void rejectsInactiveSubmission() {
        Member learner = user("학습자");
        learner.changeStatus(MemberStatus.BLOCKED);
        assertThatThrownBy(() -> Answer.builder().member(learner).question(publishedQuestion())
                .idempotencyKey(UUID.randomUUID().toString()).content("답변").build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Question publishedQuestion() {
        Question question = draftQuestion();
        question.review(admin("검수자"));
        question.publish();
        return question;
    }

    private Question draftQuestion() {
        Topic topic = Topic.builder().code("OS").name("운영체제").build();
        Concept concept = Concept.builder().topic(topic).code("PROCESS").name("프로세스").build();
        Question question = Question.builder().topic(topic).createdByMember(admin("작성자"))
                .difficulty(QuestionDifficulty.BASIC).content("문제").referenceAnswer("모범 답안").build();
        question.replaceConcepts(List.of(
                new QuestionConceptAssignment(concept, BigDecimal.ONE, true)
        ));
        return question;
    }

    private Member user(String nickname) {
        return Member.builder().nickname(nickname).build();
    }

    private Member admin(String nickname) {
        return Member.builder().nickname(nickname).role(MemberRole.ADMIN).build();
    }
}
