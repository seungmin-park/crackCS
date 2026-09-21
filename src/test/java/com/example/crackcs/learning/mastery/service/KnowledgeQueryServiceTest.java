package com.example.crackcs.learning.mastery.service;

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
import com.example.crackcs.learning.mastery.domain.KnowledgeStatus;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.learning.mastery.service.result.KnowledgeStatesResult.TopicState;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeQueryServiceTest {
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
    private KnowledgeStateService knowledgeStateService;

    @Autowired
    private EvaluationCompletionTransaction completionTransaction;

    @Autowired
    private KnowledgeQueryService knowledgeQueryService;

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
    @DisplayName("미평가 개념과 0점 개념을 구분하고 주제 신뢰도에 미평가를 포함한다")
    void aggregatesUnknownAndZero() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeChunk chunk = knowledgeChunk(topic, admin);
        concept(topic, "프로세스");
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(
                completed(member, question, concept, chunk, Verdict.INCORRECT)));

        TopicState topicState = knowledgeQueryService.knowledgeStates(member.getId()).topics().getFirst();

        assertThat(topicState.status()).isEqualTo(KnowledgeStatus.LEARNING);
        assertThat(topicState.masteryScore()).isZero();
        assertThat(topicState.confidenceScore()).isEqualTo(12.5);
        assertThat(topicState.unknownCount()).isEqualTo(1);
        assertThat(topicState.learningCount()).isEqualTo(1);
        assertThat(topicState.concepts().getFirst().masteryScore()).isZero();
        assertThat(topicState.concepts().getLast().masteryScore()).isNull();
    }

    @Test
    @DisplayName("학습 상태는 회원별로 격리하고 빈 주제와 신규 회원은 미평가로 조회한다")
    void isolatesMembersAndIncludesEmptyTopics() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeChunk chunk = knowledgeChunk(topic, admin);
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(
                completed(member, question, concept, chunk, Verdict.CORRECT)));
        Member other = memberRepository.save(Member.builder().nickname("다른 학습자").build());
        topicRepository.save(Topic.builder().code("EMPTY").name("빈 주제").build());

        List<TopicState> result = knowledgeQueryService.knowledgeStates(other.getId()).topics();

        assertThat(result).hasSize(2).allSatisfy(topicState -> {
            assertThat(topicState.status()).isEqualTo(KnowledgeStatus.UNKNOWN);
            assertThat(topicState.masteryScore()).isNull();
            assertThat(topicState.confidenceScore()).isZero();
        });
    }

    @Test
    @DisplayName("모든 활성 개념이 안정 상태일 때만 주제도 안정 상태로 표시한다")
    void aggregatesStableAndFiltersInactiveTaxonomy() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeChunk chunk = knowledgeChunk(topic, admin);
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(
                completed(member, question, concept, chunk, Verdict.CORRECT)));
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(
                completed(member, question, concept, chunk, Verdict.CORRECT)));
        completionTransaction.execute(() -> knowledgeStateService.applyInCurrentTransaction(
                completed(member, question, concept, chunk, Verdict.CORRECT)));
        Concept inactive = concept(topic, "폐기 개념");
        inactive.deactivate();
        conceptRepository.save(inactive);
        Topic hidden = topicRepository.save(Topic.builder().code("HIDDEN").name("비활성 주제").build());
        concept(hidden, "숨김");
        hidden.deactivate();
        topicRepository.save(hidden);

        List<TopicState> result = knowledgeQueryService.knowledgeStates(member.getId()).topics();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(KnowledgeStatus.STABLE);
        assertThat(result.getFirst().stableCount()).isEqualTo(1);
        assertThat(result.getFirst().concepts()).hasSize(1);
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

    private Evaluation pending(Member member, Question question) {
        Answer answer = answerRepository.save(Answer.builder().member(member).question(question)
                .idempotencyKey(UUID.randomUUID().toString()).content("스레드는 실행 단위").build());
        return evaluationRepository.save(Evaluation.builder().answer(answer).build());
    }

}
