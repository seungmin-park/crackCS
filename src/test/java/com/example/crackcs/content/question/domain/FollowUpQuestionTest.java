package com.example.crackcs.content.question.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.followup.domain.FollowUpGeneration;
import com.example.crackcs.learning.followup.domain.FollowUpResult;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowUpQuestionTest {
    @Test
    @DisplayName("원본 답변 식별자가 아직 없어도 다른 답변의 질문으로 작업을 완료할 수 없다")
    void generationRejectsDifferentTransientSource() {
        FollowUpGeneration job = FollowUpGeneration.builder().answer(source()).build();
        Question question = followUp(source());
        LocalDateTime now = LocalDateTime.now().plusSeconds(1);
        job.claim("worker", now, Duration.ofMinutes(1));
        assertThatThrownBy(() -> job.complete("worker", now, question, result(question, 1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(job.getStatus()).isEqualTo(FollowUpStatus.PROCESSING);
        assertThat(job.getQuestion()).isNull();
    }

    @Test
    @DisplayName("도메인 완료 메서드도 저장할 질문과 생성 결과의 개념 일치를 검증한다")
    void generationRejectsMismatchedConcept() {
        Answer source = source();
        Question question = followUp(source);
        FollowUpGeneration job = FollowUpGeneration.builder().answer(source).build();
        LocalDateTime now = LocalDateTime.now().plusSeconds(1);
        job.claim("worker", now, Duration.ofMinutes(1));
        assertThatThrownBy(() -> job.complete("worker", now, question, result(question, 99L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(job.getStatus()).isEqualTo(FollowUpStatus.PROCESSING);
        assertThat(job.getQuestion()).isNull();
    }

    private FollowUpResult result(Question question, Long conceptId) {
        return new FollowUpResult(question.getContent(), question.getReferenceAnswer(), conceptId, List.of(7L),
                "test", "follow-up-v1", 0, 0, 0);
    }

    @Test
    @DisplayName("후속 질문은 원본 답변과 단일 필수 개념을 가진 개인 AI 질문이다")
    void createsPrivateFollowUp() {
        Answer source = source();
        Question question = followUp(source);
        assertThat(question.getType()).isEqualTo(QuestionType.FOLLOW_UP);
        assertThat(question.getOrigin()).isEqualTo(QuestionOrigin.SYSTEM_FOLLOW_UP);
        assertThat(question.getSourceAnswer()).isSameAs(source);
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.PUBLISHED);
        assertThat(question.getCreatedAt()).isEqualTo(question.getUpdatedAt());
        assertThat(question.getQuestionConcepts()).singleElement().satisfies(concept -> {
            assertThat(concept.isRequired()).isTrue();
            assertThat(concept.getWeight()).isEqualByComparingTo("1");
        });
    }

    @Test
    @DisplayName("원본 답변 없이는 후속 질문을 만들 수 없다")
    void rejectsMissingSource() {
        assertThatThrownBy(() -> Question.followUpBuilder().content("질문").referenceAnswer("정답").build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("후속 답변으로 다시 후속 질문을 만들 수 없다")
    void rejectsSecondFollowUp() {
        Answer source = source();
        Answer next = answer(source.getMember(), followUp(source));
        assertThatThrownBy(() -> followUp(next)).isInstanceOf(InvalidContentStateException.class);
    }

    @Test
    @DisplayName("다른 회원은 도메인을 직접 호출해도 후속 답변을 만들 수 없다")
    void rejectsOtherMember() {
        Question question = followUp(source());
        assertThatThrownBy(() -> answer(Member.builder().nickname("타인").build(), question))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("후속 질문은 관리자 새 버전으로 복제할 수 없다")
    void rejectsAdminCopy() {
        Question question = followUp(source());
        assertThatThrownBy(() -> question.createNextVersion(2, admin(), QuestionDifficulty.BASIC, "복제", "정답"))
                .isInstanceOf(InvalidContentStateException.class);
    }

    private Question followUp(Answer source) {
        return Question.followUpBuilder().sourceAnswer(source)
                .concept(source.getQuestion().getQuestionConcepts().iterator().next().getConcept())
                .content("적용 질문").referenceAnswer("적용 답안").build();
    }

    private Answer source() {
        Topic topic = Topic.builder().code("OS").name("운영체제").build();
        Question question = Question.builder().topic(topic).createdByMember(admin())
                .difficulty(QuestionDifficulty.BASIC).content("원본").referenceAnswer("정답").build();
        Concept concept = Concept.builder().topic(topic).code("THREAD").name("스레드").build();
        ReflectionTestUtils.setField(concept, "id", 1L);
        question.addConcept(concept, BigDecimal.ONE, true);
        question.review(admin());
        question.publish();
        return answer(Member.builder().nickname("학습자").build(), question);
    }

    private Answer answer(Member member, Question question) {
        return Answer.builder().member(member).question(question).requestId(UUID.randomUUID().toString())
                .content("답변").build();
    }

    private Member admin() {
        return Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
    }
}
