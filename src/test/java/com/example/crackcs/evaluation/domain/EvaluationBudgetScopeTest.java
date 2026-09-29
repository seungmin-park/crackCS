package com.example.crackcs.evaluation.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.DefaultEvaluationBudgetGuard;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "crackcs.evaluation.worker-enabled=false")
@ActiveProfiles("test")
class EvaluationBudgetScopeTest {
    @Autowired EvaluationRepository evaluationRepository;
    @Autowired AnswerRepository answerRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired ConceptRepository conceptRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        evaluationRepository.deleteAll();
        answerRepository.deleteAllInBatch();
        questionRepository.deleteAll();
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("무료 로컬 평가 토큰은 OpenAI 월 예산에서 제외한다")
    void excludesLocalEvaluationsFromOpenAiBudget() {
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Topic topic = topicRepository.save(Topic.builder().code("OS").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("THREAD").name("스레드").build());
        Question question = Question.builder().topic(topic).createdByMember(admin)
                .difficulty(QuestionDifficulty.BASIC).content("스레드란?")
                .referenceAnswer("실행 단위").build();
        question.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
        question.review(admin);
        question.publish();
        question = questionRepository.save(question);
        Answer localAnswer = answerRepository.save(Answer.builder().member(learner).question(question)
                .idempotencyKey(UUID.randomUUID().toString()).content("로컬 답변").build());
        Evaluation localEvaluation = evaluationRepository.save(Evaluation.builder().answer(localAnswer).build());
        localEvaluation.complete(new EvaluationResult(Verdict.CORRECT, "정확함",
                List.of(new ConceptResult(concept.getId(), Verdict.CORRECT, "정확함")),
                List.of(), List.of(), List.of(), List.of(), "no-charge-test-provider", "test-v1", 1,
                1_000_000, 1_000_000));
        evaluationRepository.save(localEvaluation);

        DefaultEvaluationBudgetGuard budgetGuard = new DefaultEvaluationBudgetGuard(
                evaluationRepository, new BigDecimal("0.01"), new BigDecimal("2"), new BigDecimal("12"),
                "gpt-5.6-terra");

        assertThat(budgetGuard.canEvaluate()).isTrue();
    }
}
