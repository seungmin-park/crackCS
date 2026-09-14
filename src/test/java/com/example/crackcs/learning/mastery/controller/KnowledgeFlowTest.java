package com.example.crackcs.learning.mastery.controller;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.security.AuthenticatedMember;
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
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgeFlowTest {
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
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EvaluationProcessor processor;

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
    @DisplayName("신규 회원은 추천 문제를 풀고 완료 평가가 반영된 지식 지도와 학습 홈을 조회한다")
    void progressesFromRecommendationThroughCommittedEvaluation() throws Exception {
        Fixture f = fixture();
        AuthenticatedMember principal = principal(f.member());
        mvc.perform(get("/api/recommendations/next-question").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionId").value(f.question().getId()))
                .andExpect(jsonPath("$.reason").value("UNASSESSED_CONCEPT"));
        mvc.perform(get("/api/members/me/knowledge-states").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.topics[0].concepts[0].status").value("UNKNOWN"))
                .andExpect(jsonPath("$.topics[0].concepts[0].masteryScore").isEmpty());
        MvcResult submission = mvc.perform(post("/api/questions/{id}/answers", f.question().getId())
                        .with(user(principal)).with(csrf()).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("content", "스레드는 프로세스 자원을 공유하는 실행 단위다."))))
                .andExpect(status().isAccepted()).andReturn();
        Long evaluationId = mapper.readTree(submission.getResponse().getContentAsString()).get("evaluationId").asLong();
        processor.process(evaluationId);
        assertThat(states.findByMemberIdAndConceptId(f.member().getId(), f.concept().getId()).orElseThrow()
                .getAttemptCount()).isEqualTo(1);
        mvc.perform(get("/api/members/me/knowledge-states").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.topics[0].concepts[0].status").value("LEARNING"))
                .andExpect(jsonPath("$.topics[0].concepts[0].attemptCount").value(1))
                .andExpect(jsonPath("$.topics[0].concepts[0].lastEvaluatedAt").isNotEmpty());
        mvc.perform(get("/api/members/me/progress").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAnswers").value(1))
                .andExpect(jsonPath("$.recentEvaluations[0].status").value("EVALUATED"))
                .andExpect(jsonPath("$.recommendation.reason").value("LOW_MASTERY"));
        Member other = members.save(Member.builder().nickname("다른 학습자").build());
        mvc.perform(get("/api/members/me/progress").param("memberId", f.member().getId().toString())
                        .with(user(principal(other))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAnswers").value(0))
                .andExpect(jsonPath("$.topics[0].concepts[0].status").value("UNKNOWN"));
    }

    private AuthenticatedMember principal(Member member) {
        return AuthenticatedMember.from(AuthAccount.builder().member(member)
                .loginId("learner" + member.getId() + "@example.com").passwordHash("hash").build());
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

    private record Fixture(Member member, Member admin, Topic topic, Concept concept, Question question,
                           KnowledgeChunk chunk) {
    }
}
