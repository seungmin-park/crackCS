package com.example.crackcs.evaluation.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.evaluation.port.ConceptResult;
import com.example.crackcs.evaluation.port.EvaluationResult;
import com.example.crackcs.learning.domain.Answer;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluationTest {

    @Test
    @DisplayName("평가는 평가 중 상태로 생성된다")
    void startsEvaluating() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(evaluation.getVerdict()).isNull();
        assertThat(evaluation.getCreatedAt()).isEqualTo(evaluation.getUpdatedAt());
    }

    @Test
    @DisplayName("전체와 모든 Concept 결과를 원자적으로 확정한다")
    void completesWithEveryConceptResult() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();

        evaluation.complete(new EvaluationResult(Verdict.PARTIALLY_CORRECT, "보완 필요", List.of(
                new ConceptResult(11L, Verdict.CORRECT, "정확함"),
                new ConceptResult(12L, Verdict.INCORRECT, "누락됨")
        )));

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(evaluation.getScore()).isEqualTo(50);
        assertThat(evaluation.getFeedback()).isEqualTo("보완 필요");
        assertThat(evaluation.getConcepts()).extracting(EvaluationConcept::getConceptId)
                .containsExactlyInAnyOrder(11L, 12L);
        assertThat(evaluation.isKnowledgeStateEligible()).isTrue();
    }

    @Test
    @DisplayName("Concept 결과 누락과 중복 및 추가를 거부하고 평가 중 상태를 유지한다")
    void rejectsInvalidConceptResultSetAtomically() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();
        EvaluationResult invalid = new EvaluationResult(Verdict.CORRECT, "결과", List.of(
                new ConceptResult(11L, Verdict.CORRECT, "정확함"),
                new ConceptResult(11L, Verdict.CORRECT, "중복"),
                new ConceptResult(99L, Verdict.CORRECT, "추가")
        ));

        assertThatThrownBy(() -> evaluation.complete(invalid)).isInstanceOf(IllegalArgumentException.class);
        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(evaluation.getConcepts()).isEmpty();
        assertThat(evaluation.getVerdict()).isNull();
    }

    @Test
    @DisplayName("전체 정답 판정은 모든 필수 Concept이 정답일 때만 허용한다")
    void correctOverallVerdictRequiresEveryRequiredConceptToBeCorrect() {
        for (Verdict contradictoryVerdict : List.of(
                Verdict.PARTIALLY_CORRECT, Verdict.INCORRECT, Verdict.NEEDS_REVIEW
        )) {
            Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();
            EvaluationResult contradictory = new EvaluationResult(Verdict.CORRECT, "전체 정답", List.of(
                    new ConceptResult(11L, contradictoryVerdict, "필수 Concept 모순"),
                    new ConceptResult(12L, Verdict.CORRECT, "정확함")
            ));

            assertThatThrownBy(() -> evaluation.complete(contradictory))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CORRECT");
            assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
            assertThat(evaluation.getConcepts()).isEmpty();
        }
    }

    @Test
    @DisplayName("전체와 Concept 평가의 빈 피드백을 거부한다")
    void feedbackMustNotBeBlank() {
        Evaluation blankOverall = Evaluation.builder().answer(answerWithConcepts()).build();
        Evaluation blankConcept = Evaluation.builder().answer(answerWithConcepts()).build();

        assertThatThrownBy(() -> blankOverall.complete(new EvaluationResult(Verdict.CORRECT, "  ", List.of(
                new ConceptResult(11L, Verdict.CORRECT, "정확함"),
                new ConceptResult(12L, Verdict.CORRECT, "정확함")
        )))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("feedback");
        assertThatThrownBy(() -> blankConcept.complete(new EvaluationResult(Verdict.CORRECT, "정확함", List.of(
                new ConceptResult(11L, Verdict.CORRECT, " "),
                new ConceptResult(12L, Verdict.CORRECT, "정확함")
        )))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("feedback");
        assertThat(blankOverall.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(blankConcept.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
    }

    @Test
    @DisplayName("필수 Concept이 검토 필요이면 지식 상태 반영 대상이 아니다")
    void requiredNeedsReviewIsNotEligible() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();
        evaluation.complete(new EvaluationResult(Verdict.PARTIALLY_CORRECT, "검토 필요", List.of(
                new ConceptResult(11L, Verdict.NEEDS_REVIEW, "근거 부족"),
                new ConceptResult(12L, Verdict.CORRECT, "정확함")
        )));

        assertThat(evaluation.isKnowledgeStateEligible()).isFalse();
    }

    @Test
    @DisplayName("실패하면 안전한 사유를 저장하고 결과 점수는 비워 둔다")
    void failsEvaluation() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();

        evaluation.fail("외부 평가 시간 초과");

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(evaluation.getFailureReason()).isEqualTo("외부 평가 시간 초과");
        assertThat(evaluation.isKnowledgeStateEligible()).isFalse();
    }

    @Test
    @DisplayName("완료되거나 실패한 평가는 다시 전이할 수 없다")
    void terminalEvaluationCannotTransitionAgain() {
        Evaluation completed = Evaluation.builder().answer(answerWithConcepts()).build();
        completed.complete(validResult());
        Evaluation failed = Evaluation.builder().answer(answerWithConcepts()).build();
        failed.fail("실패");

        assertThatThrownBy(() -> completed.fail("재실패")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failed.complete(validResult())).isInstanceOf(IllegalStateException.class);
    }

    private EvaluationResult validResult() {
        return new EvaluationResult(Verdict.CORRECT, "정확함", List.of(
                new ConceptResult(11L, Verdict.CORRECT, "정확함"),
                new ConceptResult(12L, Verdict.CORRECT, "정확함")
        ));
    }

    private Answer answerWithConcepts() {
        Topic topic = Topic.builder().code("OS").name("운영체제").build();
        Concept required = Concept.builder().topic(topic).code("PROCESS").name("프로세스").build();
        Concept optional = Concept.builder().topic(topic).code("THREAD").name("스레드").build();
        ReflectionTestUtils.setField(required, "id", 11L);
        ReflectionTestUtils.setField(optional, "id", 12L);
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        Question question = Question.builder().topic(topic).createdByMember(admin)
                .difficulty(QuestionDifficulty.BASIC).content("차이를 설명하세요").referenceAnswer("모범 답안").build();
        question.addConcept(required, new BigDecimal("0.70"), true);
        question.addConcept(optional, new BigDecimal("0.30"), false);
        question.review(admin);
        question.publish();
        return Answer.builder().member(Member.builder().nickname("학습자").build()).question(question)
                .requestId(UUID.randomUUID().toString()).content("답변").build();
    }
}
