package com.example.crackcs.learning.recommendation.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
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
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult;
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult.Reason;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceTest {
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    private KnowledgeStateRepository knowledgeStateRepository;

    @Autowired
    private AppliedEvaluationConceptRepository appliedEvaluationConceptRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeStateService knowledgeStateService;

    @Autowired
    private EvaluationCompletionTransaction completionTransaction;

    @Autowired
    private RecommendationService recommendationService;

    @AfterEach
    void cleanUp() {
        appliedEvaluationConceptRepository.deleteAllInBatch();
        knowledgeStateRepository.deleteAllInBatch();
        evaluationRepository.deleteAll();
        answerRepository.deleteAllInBatch();
        questionRepository.deleteAll();
        knowledgeChunkRepository.deleteAllInBatch();
        knowledgeDocumentRepository.deleteAllInBatch();
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("문제에 활성 개념이 있어도 다른 연결 개념이 비활성이면 문제 전체를 추천에서 제외한다")
    void excludesQuestionWithMixedActiveAndInactiveConcepts() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question originalQuestion = question(admin, topic, concept, "스레드는 무엇인가요?");
        knowledgeChunk(topic, admin);
        originalQuestion.retire();
        questionRepository.save(originalQuestion);
        Concept disabled = concept(topic, "비활성 개념");
        Question question = Question.builder().topic(topic).createdByMember(admin)
                .difficulty(QuestionDifficulty.BASIC).content("복합 문제").referenceAnswer("답").build();
        question.replaceConcepts(List.of(
                new QuestionConceptAssignment(concept, new BigDecimal("0.5"), true),
                new QuestionConceptAssignment(disabled, new BigDecimal("0.5"), true)
        ));
        question.review(admin);
        question.publish();
        questionRepository.save(question);
        disabled.deactivate();
        conceptRepository.save(disabled);
        assertThat(recommendationService.recommendation(member.getId()).reason()).isEqualTo(Reason.NO_AVAILABLE_QUESTION);
    }

    @Test
    @DisplayName("추천 후보 안에서는 미평가 개념을 숙련도가 낮은 개념보다 먼저 선택한다")
    void recommendsUnassessedBeforeLowMastery() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question originalQuestion = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeChunk chunk = knowledgeChunk(topic, admin);
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(
                completed(member, originalQuestion, concept, chunk, Verdict.INCORRECT)));
        Concept unseen = concept(topic, "프로세스");
        Question question = question(admin, topic, unseen, "프로세스 문제");

        RecommendationResult result = recommendationService.recommendation(member.getId());

        assertThat(result.questionId()).isEqualTo(question.getId());
        assertThat(result.conceptId()).isEqualTo(unseen.getId());
        assertThat(result.reason()).isEqualTo(Reason.UNASSESSED_CONCEPT);
    }

    @Test
    @DisplayName("미평가 개념에 풀 수 있는 문제가 없으면 평가된 개념의 문제를 추천한다")
    void skipsUnassessedWithoutAvailableQuestion() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeChunk chunk = knowledgeChunk(topic, admin);
        concept(topic, "문제가 없는 개념");
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(
                completed(member, question, concept, chunk, Verdict.INCORRECT)));

        RecommendationResult result = recommendationService.recommendation(member.getId());

        assertThat(result.questionId()).isEqualTo(question.getId());
        assertThat(result.reason()).isEqualTo(Reason.LOW_MASTERY);
    }

    @Test
    @DisplayName("숙련도가 같으면 미풀이 문제 다음 오래전에 푼 문제와 문제 식별자 순으로 추천한다")
    void breaksTiesByLastAnswerThenQuestionId() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question first = question(admin, topic, concept, "스레드는 무엇인가요?");
        knowledgeChunk(topic, admin);
        Question second = question(admin, topic, concept, "두 번째 문제");
        Question third = question(admin, topic, concept, "세 번째 문제");
        Evaluation firstAnswer = pending(member, first);
        assertThat(recommendationService.recommendation(member.getId()).questionId()).isEqualTo(second.getId());
        pending(member, second);
        Evaluation thirdAnswer = pending(member, third);
        // submittedAt은 생성 시 확정되는 불변 값이므로 과거 풀이 이력의 시간 경계만 SQL로 준비한다.
        jdbcTemplate.update("update answer set submitted_at = ? where id = ?", LocalDateTime.now().minusDays(2),
                thirdAnswer.getAnswer().getId());
        jdbcTemplate.update("update answer set submitted_at = ? where id = ?", LocalDateTime.now().minusDays(1),
                firstAnswer.getAnswer().getId());
        assertThat(recommendationService.recommendation(member.getId()).questionId()).isEqualTo(third.getId());
    }

    @Test
    @DisplayName("문제에 연결된 개념 중 미평가와 개념 식별자 순으로 추천 이유를 선택한다")
    void choosesBestConceptWithinQuestion() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question originalQuestion = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeChunk chunk = knowledgeChunk(topic, admin);
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(
                completed(member, originalQuestion, concept, chunk, Verdict.CORRECT)));
        Concept second = concept(topic, "두 번째 개념");
        Concept third = concept(topic, "세 번째 개념");
        // Published questions are immutable, so a new reviewed question owns this concept set.
        Question question = Question.builder().topic(topic).createdByMember(admin)
                .difficulty(QuestionDifficulty.BASIC)
                .content("여러 개념 문제").referenceAnswer("답").build();
        question.replaceConcepts(List.of(
                new QuestionConceptAssignment(concept, new BigDecimal("0.34"), true),
                new QuestionConceptAssignment(third, new BigDecimal("0.33"), true),
                new QuestionConceptAssignment(second, new BigDecimal("0.33"), true)
        ));
        question.review(admin);
        question.publish();
        questionRepository.save(question);
        RecommendationResult result = recommendationService.recommendation(member.getId());
        assertThat(result.questionId()).isEqualTo(question.getId());
        assertThat(result.conceptId()).isEqualTo(second.getId());
    }

    @Test
    @DisplayName("폐기 문제와 비활성 개념 또는 주제를 연결한 문제는 추천하지 않는다")
    void excludesUnavailableQuestions() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question originalQuestion = question(admin, topic, concept, "스레드는 무엇인가요?");
        knowledgeChunk(topic, admin);
        originalQuestion.retire();
        questionRepository.save(originalQuestion);
        Concept inactiveConcept = concept(topic, "비활성");
        question(admin, topic, inactiveConcept, "비활성 개념 문제");
        inactiveConcept.deactivate();
        conceptRepository.save(inactiveConcept);
        Topic otherTopic = topicRepository.save(Topic.builder().code("DISABLED").name("비활성").build());
        question(admin, otherTopic, concept(otherTopic, "개념"), "비활성 주제 문제");
        otherTopic.deactivate();
        topicRepository.save(otherTopic);
        RecommendationResult result = recommendationService.recommendation(member.getId());
        assertThat(result.reason()).isEqualTo(Reason.NO_AVAILABLE_QUESTION);
        assertThat(result.questionId()).isNull();
        assertThat(result.title()).isNull();
        assertThat(result.conceptId()).isNull();
        assertThat(result.conceptName()).isNull();
        assertThat(result.reasonText()).isNotBlank();
    }

    private KnowledgeChunk knowledgeChunk(Topic topic, Member admin) {
        KnowledgeDocument document = KnowledgeDocument.builder().topic(topic).createdByMember(admin)
                .title("스레드 근거").sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성")
                .content("스레드는 프로세스 자원을 공유하는 실행 단위다.").build();
        document.review(admin);
        document.publish();
        document = knowledgeDocumentRepository.save(document);
        return knowledgeChunkRepository.save(KnowledgeChunk.create(document, 0, 0, document.getContent().length(),
                document.getContent(), "test-v1"));
    }

    private Concept concept(Topic topic, String name) {
        return conceptRepository.save(Concept.builder().topic(topic).code(UUID.randomUUID().toString()).name(name).build());
    }

    private Question question(Member admin, Topic topic, Concept concept, String content) {
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content(content).referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.replaceConcepts(List.of(
                new QuestionConceptAssignment(concept, BigDecimal.ONE, true)
        ));
        question.review(admin);
        question.publish();
        return questionRepository.save(question);
    }

    private Evaluation pending(Member member, Question question) {
        Answer answer = answerRepository.save(Answer.builder().member(member).question(question)
                .idempotencyKey(UUID.randomUUID().toString()).content("스레드는 실행 단위").build());
        return evaluationRepository.save(Evaluation.builder().answer(answer).build());
    }

    private Long completed(Member member, Question question, Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        Long id = pending(member, question).getId();
        complete(id, concept, chunk, verdict);
        return id;
    }

    private void complete(Long id, Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        transactionTemplate.executeWithoutResult(status -> {
            Evaluation evaluation = evaluationRepository.findById(id).orElseThrow();
            evaluation.completeWithEvidence(result(concept, chunk, verdict),
                    List.of(knowledgeChunkRepository.findById(chunk.getId()).orElseThrow()));
        });
    }

    private EvaluationResult result(Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        return new EvaluationResult(verdict, "평가 완료",
                List.of(new ConceptResult(concept.getId(), verdict, "개념 평가")),
                List.of(), List.of(), List.of(), List.of(chunk.getId()), "test", "v1", 1, 1, 1);
    }
}
