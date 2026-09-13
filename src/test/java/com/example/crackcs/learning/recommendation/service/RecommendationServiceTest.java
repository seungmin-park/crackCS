package com.example.crackcs.learning.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.domain.ConceptResult;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationCompletionTransaction;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.learning.mastery.service.KnowledgeStateService;
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult.Reason;
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceTest {
    @Autowired
    private MemberRepository members;

    @Autowired
    private TopicRepository topics;

    @Autowired
    private ConceptRepository concepts;

    @Autowired
    private QuestionRepository questions;

    @Autowired
    private AnswerRepository answers;

    @Autowired
    private EvaluationRepository evaluations;

    @Autowired
    private KnowledgeDocumentRepository documents;

    @Autowired
    private KnowledgeChunkRepository chunks;

    @Autowired
    private KnowledgeStateRepository states;

    @Autowired
    private AppliedEvaluationConceptRepository appliedConcepts;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private KnowledgeStateService knowledge;

    @Autowired
    private EvaluationCompletionTransaction completionTransaction;

    @Autowired
    private RecommendationService recommendations;

    @AfterEach
    void cleanUp() {
        appliedConcepts.deleteAllInBatch();
        states.deleteAllInBatch();
        evaluations.deleteAll();
        answers.deleteAllInBatch();
        questions.deleteAll();
        chunks.deleteAllInBatch();
        documents.deleteAllInBatch();
        concepts.deleteAllInBatch();
        topics.deleteAllInBatch();
        members.deleteAllInBatch();
    }

    @Test
    @DisplayName("문제에 활성 개념이 있어도 다른 연결 개념이 비활성이면 문제 전체를 추천에서 제외한다")
    void excludesQuestionWithMixedActiveAndInactiveConcepts() {
        Fixture f = fixture();
        f.question().retire();
        questions.save(f.question());
        Concept disabled = concept(f.topic(), "비활성 개념");
        Question question = Question.builder().topic(f.topic()).createdByMember(f.admin())
                .difficulty(QuestionDifficulty.BASIC).content("복합 문제").referenceAnswer("답").build();
        question.addConcept(f.concept(), new BigDecimal("0.5"), true);
        question.addConcept(disabled, new BigDecimal("0.5"), true);
        question.review(f.admin());
        question.publish();
        questions.save(question);
        disabled.deactivate();
        concepts.save(disabled);
        assertThat(recommendations.recommendation(f.member().getId()).reason()).isEqualTo(Reason.NO_AVAILABLE_QUESTION);
    }

    @Test
    @DisplayName("추천 후보 안에서는 미평가 개념을 숙련도가 낮은 개념보다 먼저 선택한다")
    void recommendsUnassessedBeforeLowMastery() {
        Fixture f = fixture();
        completionTransaction.execute(() -> knowledge.applyInCurrentTransaction(completed(f, Verdict.INCORRECT)));
        Concept unseen = concept(f.topic(), "프로세스");
        Question question = question(f.admin(), f.topic(), unseen, "프로세스 문제");
        RecommendationResult result = recommendations.recommendation(f.member().getId());
        assertThat(result.questionId()).isEqualTo(question.getId());
        assertThat(result.conceptId()).isEqualTo(unseen.getId());
        assertThat(result.reason()).isEqualTo(Reason.UNASSESSED_CONCEPT);
    }

    @Test
    @DisplayName("미평가 개념에 풀 수 있는 문제가 없으면 평가된 개념의 문제를 추천한다")
    void skipsUnassessedWithoutAvailableQuestion() {
        Fixture f = fixture();
        concept(f.topic(), "문제가 없는 개념");
        completionTransaction.execute(() -> knowledge.applyInCurrentTransaction(completed(f, Verdict.INCORRECT)));
        RecommendationResult result = recommendations.recommendation(f.member().getId());
        assertThat(result.questionId()).isEqualTo(f.question().getId());
        assertThat(result.reason()).isEqualTo(Reason.LOW_MASTERY);
    }

    @Test
    @DisplayName("숙련도가 같으면 미풀이 문제 다음 오래전에 푼 문제와 문제 식별자 순으로 추천한다")
    void breaksTiesByLastAnswerThenQuestionId() {
        Fixture f = fixture();
        Question second = question(f.admin(), f.topic(), f.concept(), "두 번째 문제");
        Question third = question(f.admin(), f.topic(), f.concept(), "세 번째 문제");
        Evaluation firstAnswer = pending(f);
        assertThat(recommendations.recommendation(f.member().getId()).questionId()).isEqualTo(second.getId());
        pending(f.member(), second);
        Evaluation thirdAnswer = pending(f.member(), third);
        // submittedAt은 생성 시 확정되는 불변 값이므로 과거 풀이 이력의 시간 경계만 SQL로 준비한다.
        jdbc.update("update answer set submitted_at = ? where id = ?", LocalDateTime.now().minusDays(2),
                thirdAnswer.getAnswer().getId());
        jdbc.update("update answer set submitted_at = ? where id = ?", LocalDateTime.now().minusDays(1),
                firstAnswer.getAnswer().getId());
        assertThat(recommendations.recommendation(f.member().getId()).questionId()).isEqualTo(third.getId());
    }

    @Test
    @DisplayName("문제에 연결된 개념 중 미평가와 개념 식별자 순으로 추천 이유를 선택한다")
    void choosesBestConceptWithinQuestion() {
        Fixture f = fixture();
        completionTransaction.execute(() -> knowledge.applyInCurrentTransaction(completed(f, Verdict.CORRECT)));
        Concept second = concept(f.topic(), "두 번째 개념");
        Concept third = concept(f.topic(), "세 번째 개념");
        // Published questions are immutable, so a new reviewed question owns this concept set.
        Question question = Question.builder().topic(f.topic()).createdByMember(f.admin())
                .difficulty(QuestionDifficulty.BASIC)
                .content("여러 개념 문제").referenceAnswer("답").build();
        question.addConcept(f.concept(), new BigDecimal("0.34"), true);
        question.addConcept(third, new BigDecimal("0.33"), true);
        question.addConcept(second, new BigDecimal("0.33"), true);
        question.review(f.admin());
        question.publish();
        questions.save(question);
        RecommendationResult result = recommendations.recommendation(f.member().getId());
        assertThat(result.questionId()).isEqualTo(question.getId());
        assertThat(result.conceptId()).isEqualTo(second.getId());
    }

    @Test
    @DisplayName("폐기 문제와 비활성 개념 또는 주제를 연결한 문제는 추천하지 않는다")
    void excludesUnavailableQuestions() {
        Fixture f = fixture();
        f.question().retire();
        questions.save(f.question());
        Concept inactiveConcept = concept(f.topic(), "비활성");
        question(f.admin(), f.topic(), inactiveConcept, "비활성 개념 문제");
        inactiveConcept.deactivate();
        concepts.save(inactiveConcept);
        Topic otherTopic = topics.save(Topic.builder().code("DISABLED").name("비활성").build());
        question(f.admin(), otherTopic, concept(otherTopic, "개념"), "비활성 주제 문제");
        otherTopic.deactivate();
        topics.save(otherTopic);
        RecommendationResult result = recommendations.recommendation(f.member().getId());
        assertThat(result.reason()).isEqualTo(Reason.NO_AVAILABLE_QUESTION);
        assertThat(result.questionId()).isNull();
        assertThat(result.title()).isNull();
        assertThat(result.conceptId()).isNull();
        assertThat(result.conceptName()).isNull();
        assertThat(result.reasonText()).isNotBlank();
    }

    private Fixture fixture() {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Member admin = members.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topics.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeDocument document = KnowledgeDocument.builder().topic(topic).createdByMember(admin)
                .title("스레드 근거").sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성")
                .content("스레드는 프로세스 자원을 공유하는 실행 단위다.").build();
        document.review(admin);
        document.publish();
        document = documents.save(document);
        KnowledgeChunk chunk = chunks.save(KnowledgeChunk.create(document, 0, 0, document.getContent().length(),
                document.getContent(), "test-v1"));
        return new Fixture(member, admin, topic, concept, question, chunk);
    }

    private Concept concept(Topic topic, String name) {
        return concepts.save(Concept.builder().topic(topic).code(UUID.randomUUID().toString()).name(name).build());
    }

    private Question question(Member admin, Topic topic, Concept concept, String content) {
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content(content).referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.addConcept(concept, BigDecimal.ONE, true);
        question.review(admin);
        question.publish();
        return questions.save(question);
    }

    private Evaluation pending(Fixture fixture) {
        return pending(fixture.member(), fixture.question());
    }

    private Evaluation pending(Member member, Question question) {
        Answer answer = answers.save(Answer.builder().member(member).question(question)
                .requestId(UUID.randomUUID().toString()).content("스레드는 실행 단위").build());
        return evaluations.save(Evaluation.builder().answer(answer).build());
    }

    private Long completed(Fixture fixture, Verdict verdict) {
        Long id = pending(fixture).getId();
        complete(id, fixture, verdict);
        return id;
    }

    private void complete(Long id, Fixture fixture, Verdict verdict) {
        transactions.executeWithoutResult(status -> {
            Evaluation evaluation = evaluations.findById(id).orElseThrow();
            evaluation.completeWithEvidence(result(fixture, verdict),
                    List.of(chunks.findById(fixture.chunk().getId()).orElseThrow()));
        });
    }

    private EvaluationResult result(Fixture fixture, Verdict verdict) {
        return new EvaluationResult(verdict, "평가 완료",
                List.of(new ConceptResult(fixture.concept().getId(), verdict, "개념 평가")),
                List.of(), List.of(), List.of(), List.of(fixture.chunk().getId()), "test", "v1", 1, 1, 1);
    }

    private record Fixture(Member member, Member admin, Topic topic, Concept concept, Question question,
                           KnowledgeChunk chunk) {
    }
}
