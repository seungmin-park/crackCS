package com.example.crackcs.evaluation.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
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
import java.time.Duration;
import java.time.LocalDateTime;
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
        for (Verdict contradictoryVerdict : List.of(Verdict.PARTIALLY_CORRECT, Verdict.INCORRECT)) {
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
    @DisplayName("필수 Concept이 검토 필요이면 전체 판정도 검토 필요여야 한다")
    void requiredNeedsReviewRequiresOverallNeedsReview() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();

        assertThatThrownBy(() -> evaluation.complete(new EvaluationResult(Verdict.PARTIALLY_CORRECT, "검토 필요", List.of(
                new ConceptResult(11L, Verdict.NEEDS_REVIEW, "근거 부족"),
                new ConceptResult(12L, Verdict.CORRECT, "정확함")
        )))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("NEEDS_REVIEW");

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(evaluation.getConcepts()).isEmpty();
    }

    @Test
    @DisplayName("전체와 필수 Concept이 검토 필요이면 지식 상태 반영 대상이 아니다")
    void consistentNeedsReviewIsNotEligible() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();
        evaluation.complete(new EvaluationResult(Verdict.NEEDS_REVIEW, "검토 필요", List.of(
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

    @Test
    @DisplayName("평가에 전달하지 않은 Chunk 인용은 결과 전체를 거부한다")
    void rejectsEvidenceThatWasNotProvidedToEvaluator() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();
        KnowledgeChunk provided = chunk(21L, "프로세스는 자원을 소유한다.");
        EvaluationResult result = detailedResult(List.of(22L));

        assertThatThrownBy(() -> evaluation.completeWithEvidence(result, List.of(provided)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("evidence must reference only provided chunks");

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(evaluation.getEvidence()).isEmpty();
    }

    @Test
    @DisplayName("검증된 결과는 실제 전달 Chunk와 모델 실행 정보를 함께 확정한다")
    void completesWithProvidedEvidenceAndExecutionMetadata() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();
        KnowledgeChunk provided = chunk(21L, "프로세스는 자원을 소유한다.");

        evaluation.completeWithEvidence(detailedResult(List.of(21L)), List.of(provided));

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(evaluation.getEvidence()).extracting(EvaluationEvidence::getChunkId).containsExactly(21L);
        assertThat(evaluation.getModelName()).isEqualTo("gpt-5.6-luna");
        assertThat(evaluation.getEvaluatorVersion()).isEqualTo("os-evaluator-v1");
        assertThat(evaluation.getProcessingDurationMillis()).isEqualTo(1200L);
        assertThat(evaluation.getStrengths()).containsExactly("프로세스의 자원 소유를 설명함");
    }

    @Test
    @DisplayName("근거가 부족한 평가는 성공 점수 없이 검토 필요 상태로 확정한다")
    void marksInsufficientEvidenceAsNeedsReview() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();

        evaluation.requireReview("EVIDENCE_NOT_FOUND");

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.NEEDS_REVIEW);
        assertThat(evaluation.getScore()).isNull();
        assertThat(evaluation.isKnowledgeStateEligible()).isFalse();
    }

    @Test
    @DisplayName("평가 작업 lease는 중복 선점을 막고 만료 뒤 다른 worker가 복구한다")
    void claimsWorkAndRecoversExpiredLease() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();
        LocalDateTime now = evaluation.getCreatedAt().plusSeconds(1);

        assertThat(evaluation.claim("worker-a", now, Duration.ofSeconds(30))).isTrue();
        assertThat(evaluation.claim("worker-b", now.plusSeconds(10), Duration.ofSeconds(30))).isFalse();
        assertThat(evaluation.claim("worker-b", now.plusSeconds(31), Duration.ofSeconds(30))).isTrue();

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.PROCESSING);
        assertThat(evaluation.getLeaseOwner()).isEqualTo("worker-b");
        assertThat(evaluation.getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("실패한 시도는 다음 실행 시각을 저장하고 대기 상태로 되돌린다")
    void schedulesPersistedRetry() {
        Evaluation evaluation = Evaluation.builder().answer(answerWithConcepts()).build();
        LocalDateTime now = evaluation.getCreatedAt().plusSeconds(1);
        evaluation.claim("worker-a", now, Duration.ofSeconds(30));

        evaluation.scheduleRetry("PROVIDER_TIMEOUT", now, Duration.ofSeconds(2));

        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(evaluation.getNextAttemptAt()).isEqualTo(now.plusSeconds(2));
        assertThat(evaluation.getLeaseOwner()).isNull();
        assertThat(evaluation.getFailureReason()).isEqualTo("PROVIDER_TIMEOUT");
    }

    private EvaluationResult validResult() {
        return new EvaluationResult(Verdict.CORRECT, "정확함", List.of(
                new ConceptResult(11L, Verdict.CORRECT, "정확함"),
                new ConceptResult(12L, Verdict.CORRECT, "정확함")
        ));
    }

    private EvaluationResult detailedResult(List<Long> evidenceIds) {
        return new EvaluationResult(
                Verdict.CORRECT,
                "정확함",
                List.of(
                        new ConceptResult(11L, Verdict.CORRECT, "정확함"),
                        new ConceptResult(12L, Verdict.CORRECT, "정확함")
                ),
                List.of("프로세스의 자원 소유를 설명함"),
                List.of(),
                List.of(),
                evidenceIds,
                "gpt-5.6-luna",
                "os-evaluator-v1",
                1200L,
                800L,
                200L
        );
    }

    private KnowledgeChunk chunk(Long id, String content) {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .topic(Topic.builder().code("OS_EVIDENCE").name("운영체제").build())
                .createdByMember(Member.builder().nickname("문서 관리자").role(MemberRole.ADMIN).build())
                .title("프로세스")
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general")
                .licenseNote("독립 작성")
                .content(content)
                .build();
        document.review(Member.builder().nickname("검수 관리자").role(MemberRole.ADMIN).build());
        document.publish();
        KnowledgeChunk chunk = KnowledgeChunk.create(document, 0, 0, content.length(), content, "policy-v1");
        ReflectionTestUtils.setField(chunk, "id", id);
        return chunk;
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
