package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.evaluation.domain.ConceptResult;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowUpSourcePolicyTest {
    static Stream<Arguments> selectionCases() {
        return Stream.of(
                Arguments.of(Verdict.INCORRECT, Verdict.INCORRECT, Verdict.CORRECT, false, "0.40", 10L),
                Arguments.of(Verdict.PARTIALLY_CORRECT, Verdict.CORRECT, Verdict.PARTIALLY_CORRECT, false, "0.40", 30L),
                Arguments.of(Verdict.CORRECT, Verdict.CORRECT, Verdict.CORRECT, false, "0.40", 20L),
                Arguments.of(Verdict.CORRECT, Verdict.CORRECT, Verdict.CORRECT, true, "0.40", 10L),
                Arguments.of(Verdict.CORRECT, Verdict.CORRECT, Verdict.CORRECT, false, "0.30", 30L),
                Arguments.of(Verdict.INCORRECT, Verdict.CORRECT, Verdict.CORRECT, false, "0.40", 20L));
    }

    @Test
    @DisplayName("평가 존재 여부가 확인되지 않은 정책 호출을 거부한다")
    void rejectsMissingEvaluation() {
        FollowUpSourcePolicy policy = new FollowUpSourcePolicy();

        assertThatThrownBy(() -> policy.findUnavailabilityReason(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("selectionCases")
    @DisplayName("전체 판정 목적을 유지하며 일치 판정 필수 여부 가중치 식별자 순으로 개념을 선택한다")
    void selectsConceptInPriorityOrder(Verdict overall, Verdict firstVerdict, Verdict thirdVerdict,
                                       boolean firstRequired, String secondWeight, long expectedId) {
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        Topic topic = Topic.builder().code("OS").name("운영체제").build();
        ReflectionTestUtils.setField(topic, "id", 1L);
        Concept first = concept(topic, 10L);
        Concept second = concept(topic, 20L);
        Concept third = concept(topic, 30L);
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content("원본 문제").referenceAnswer("모범 답안").build();
        question.replaceConcepts(List.of(
                new QuestionConceptAssignment(first, new BigDecimal("0.20"), firstRequired),
                new QuestionConceptAssignment(second, new BigDecimal(secondWeight), !firstRequired),
                new QuestionConceptAssignment(third, new BigDecimal("0.80").subtract(new BigDecimal(secondWeight)), !firstRequired)
        ));
        question.review(admin);
        question.publish();
        Answer answer = Answer.builder().member(Member.builder().nickname("학습자").build()).question(question)
                .requestId(UUID.randomUUID().toString()).content("전송하지 않을 원문").build();
        KnowledgeDocument document = KnowledgeDocument.builder().topic(topic).createdByMember(admin)
                .title("공개 근거").content("공개 근거 원문").sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("직접 작성").build();
        document.review(admin);
        document.publish();
        KnowledgeChunk chunk = KnowledgeChunk.create(document, 0, 0, document.getContent().length(),
                document.getContent(), "v1");
        ReflectionTestUtils.setField(chunk, "id", 7L);
        Evaluation evaluation = Evaluation.builder().answer(answer).build();
        evaluation.completeWithEvidence(new EvaluationResult(overall, "피드백", List.of(
                new ConceptResult(10L, firstVerdict, "첫 번째"),
                new ConceptResult(20L, Verdict.CORRECT, "두 번째"),
                new ConceptResult(30L, thirdVerdict, "세 번째")), List.of(), List.of("누락"), List.of("오개념"),
                List.of(7L), "test", "v1", 0, 0, 0), List.of(chunk));

        FollowUpSourcePolicy policy = new FollowUpSourcePolicy();
        FollowUpRequest request = policy.request(evaluation);
        assertThat(policy.findUnavailabilityReason(evaluation)).isEmpty();
        assertThat(request.conceptId()).isEqualTo(expectedId);
        assertThat(request.purpose()).isEqualTo(overall);
        assertThat(request.evidence()).containsExactly(new FollowUpRequest.Evidence(7L, "공개 근거 원문"));
        assertThat(request.omissions()).containsExactly("누락");
        assertThat(request.misconceptions()).containsExactly("오개념");
    }

    private Concept concept(Topic topic, Long id) {
        Concept concept = Concept.builder().topic(topic).code("CONCEPT_" + id).name("개념 " + id).build();
        ReflectionTestUtils.setField(concept, "id", id);
        return concept;
    }
}
