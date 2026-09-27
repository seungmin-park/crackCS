package com.example.crackcs.learning.answer.service;

import com.example.crackcs.content.knowledge.service.KnowledgeDocumentService;
import com.example.crackcs.content.knowledge.service.KnowledgeDocumentDraft;
import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkService;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.evaluation.retrieval.KnowledgeRetrievalService;
import com.example.crackcs.evaluation.retrieval.RetrievalQuery;
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
import com.example.crackcs.learning.answer.controller.response.EvaluationResponse;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.answer.service.result.AnswerEvaluationResult;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeAnswerSerializationTest {
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
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired private KnowledgeDocumentService knowledgeDocumentService;
    @Autowired private KnowledgeChunkService knowledgeChunkService;
    @Autowired private KnowledgeRetrievalService knowledgeRetrievalService;

    @AfterEach
    void cleanUp() {
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
    @DisplayName("평가 조회 응답은 Service 트랜잭션 종료 후에도 근거와 피드백 목록을 직렬화한다")
    void serializesFeedbackOutsideTransaction() {
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code(UUID.randomUUID().toString()).name("운영체제").build());
        Concept concept = concept(topic, "스레드");
        Question question = question(admin, topic, concept, "스레드는 무엇인가요?");
        KnowledgeChunk chunk = knowledgeChunk(topic, admin);
        Long id = saveCompletedEvaluation(learner, question, concept, chunk, Verdict.CORRECT);
        Long answerId = evaluationRepository.findById(id).orElseThrow().getAnswer().getId();

        AnswerEvaluationResult answerEvaluationResult = answerService.findEvaluation(learner.getId(), answerId);
        String json = objectMapper.writeValueAsString(EvaluationResponse.from(answerEvaluationResult));

        assertThat(json).contains("\"strengths\":[]", "\"omissions\":[]", "\"misconceptions\":[]", "\"evidence\":[{");
    }

    @Test
    @DisplayName("문서를 새 버전으로 교체한 뒤에도 과거 평가의 근거는 원본을 유지하고 새 검색은 새 버전만 반환한다")
    void preservesHistoricalEvidenceAfterDocumentReplacement() {
        Member learner = memberRepository.save(Member.builder().nickname("학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("EVIDENCE_VERSION").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("THREAD").name("스레드").build());
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content("스레드는 무엇인가요?").referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
        question.review(admin);
        question.publish();
        question = questionRepository.save(question);
        KnowledgeDocument original = knowledgeDocumentService.create(admin.getId(), new KnowledgeDocumentDraft(
                topic.getId(), "스레드 원본 근거", KnowledgeSourceType.INTERNAL_SUMMARY, null, "general", "직접 작성",
                "스레드는 프로세스 자원을 공유하는 실행 단위다."));
        knowledgeDocumentService.review(original.getId(), admin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(original.getId());
        KnowledgeChunk originalChunk = knowledgeChunkService.generateChunks(original.getId()).chunks().getFirst();
        Answer answer = answerRepository.save(Answer.builder().member(learner).question(question)
                .idempotencyKey(UUID.randomUUID().toString()).content("스레드는 프로세스 자원을 공유합니다.").build());
        Evaluation evaluation = evaluationRepository.save(Evaluation.builder().answer(answer).build());
        transactionTemplate.executeWithoutResult(status -> {
            Evaluation pending = evaluationRepository.findById(evaluation.getId()).orElseThrow();
            pending.completeWithEvidence(new EvaluationResult(Verdict.CORRECT, "평가 완료",
                    List.of(new ConceptResult(concept.getId(), Verdict.CORRECT, "개념 평가")),
                    List.of(), List.of(), List.of(), List.of(originalChunk.getId()), "test", "v1", 1, 1, 1),
                    List.of(knowledgeChunkRepository.findById(originalChunk.getId()).orElseThrow()));
        });
        AnswerEvaluationResult before = answerService.findEvaluation(learner.getId(), answer.getId());

        KnowledgeDocument replacement = knowledgeDocumentService.createNextVersion(original.getId(), admin.getId(),
                new KnowledgeDocumentDraft(topic.getId(), "스레드 새 근거", KnowledgeSourceType.INTERNAL_SUMMARY, null,
                        "general", "직접 작성", "스레드는 프로세스 자원을 공유하는 실행 단위이며 각자 스택을 가진다."));
        knowledgeDocumentService.review(replacement.getId(), admin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(replacement.getId());
        KnowledgeChunk replacementChunk = knowledgeChunkService.generateChunks(replacement.getId()).chunks().getFirst();

        AnswerEvaluationResult after = answerService.findEvaluation(learner.getId(), answer.getId());
        assertThat(after).isEqualTo(before);
        assertThat(after.evidence()).hasSize(1);
        assertThat(after.evidence().getFirst().chunkId()).isEqualTo(originalChunk.getId());
        assertThat(after.evidence().getFirst().documentVersion()).isEqualTo(1);
        assertThat(after.evidence().getFirst().content()).isEqualTo(originalChunk.getContent());
        assertThat(knowledgeDocumentRepository.findById(original.getId()).orElseThrow().getStatus())
                .isEqualTo(KnowledgeDocumentStatus.RETIRED);
        assertThat(knowledgeRetrievalService.retrieve(new RetrievalQuery(topic.getId(), List.of("스레드"),
                question.getContent(), question.getReferenceAnswer(), answer.getContent()), 5).chunks())
                .extracting(result -> result.chunk().getId()).containsExactly(replacementChunk.getId());
        assertThat(objectMapper.writeValueAsString(EvaluationResponse.from(after)))
                .contains("스레드 원본 근거").doesNotContain("스레드 새 근거");
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

    private Long saveCompletedEvaluation(Member learner, Question question, Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        Long id = savePendingEvaluation(learner, question).getId();
        completeEvaluation(id, concept, chunk, verdict);
        return id;
    }

    private void completeEvaluation(Long id, Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        transactionTemplate.executeWithoutResult(status -> {
            Evaluation evaluation = evaluationRepository.findById(id).orElseThrow();
            evaluation.completeWithEvidence(evaluationResult(concept, chunk, verdict),
                    List.of(knowledgeChunkRepository.findById(chunk.getId()).orElseThrow()));
        });
    }

    private EvaluationResult evaluationResult(Concept concept, KnowledgeChunk chunk, Verdict verdict) {
        return new EvaluationResult(verdict, "평가 완료",
                List.of(new ConceptResult(concept.getId(), verdict, "개념 평가")),
                List.of(), List.of(), List.of(), List.of(chunk.getId()), "test", "v1", 1, 1, 1);
    }

    private Evaluation savePendingEvaluation(Member learner, Question question) {
        Answer answer = answerRepository.save(Answer.builder().member(learner).question(question)
                .idempotencyKey(UUID.randomUUID().toString()).content("스레드는 실행 단위").build());
        return evaluationRepository.save(Evaluation.builder().answer(answer).build());
    }

}
