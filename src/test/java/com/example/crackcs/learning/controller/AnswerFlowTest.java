package com.example.crackcs.learning.controller;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionConceptRepository;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.learning.repository.AnswerRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "crackcs.evaluation.worker-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnswerFlowTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    MemberRepository members;
    @Autowired
    TopicRepository topics;
    @Autowired
    ConceptRepository concepts;
    @Autowired
    QuestionRepository questions;
    @Autowired
    QuestionConceptRepository questionConcepts;

    @Autowired
    EvaluationRepository evaluations;
    @Autowired
    AnswerRepository answers;
    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void tearDown() {
        jdbc.update("delete from evaluation_concept");
        evaluations.deleteAllInBatch();
        answers.deleteAllInBatch();
        questionConcepts.deleteAllInBatch();
        questions.deleteAllInBatch();
        concepts.deleteAllInBatch();
        topics.deleteAllInBatch();
        members.deleteAllInBatch();
    }

    @Test
    @DisplayName("공개 문제에 답변을 제출하면 평가 진행 상태를 반환한다")
    void submitsAnswerForEvaluation() throws Exception {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        mvc.perform(post("/api/questions/{id}/answers", question.getId())
                        .with(user(principal(member))).with(csrf())
                        .contentType("application/json")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(mapper.writeValueAsString(Map.of("content", "답변 원문"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.content").value("답변 원문"))
                .andExpect(jsonPath("$.evaluation.status").value("EVALUATING"));
    }

    @Test
    @DisplayName("같은 요청을 동시에 제출해도 답변과 평가가 한 개씩만 저장된다")
    void deduplicatesConcurrentRequests() throws Exception {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        String key = UUID.randomUUID().toString();
        String body = mapper.writeValueAsString(Map.of("content", "같은 원문"));
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Long> submit = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                String response = mvc.perform(post("/api/questions/{id}/answers", question.getId())
                                .with(user(principal(member))).with(csrf()).header("Idempotency-Key", key)
                                .contentType("application/json").content(body))
                        .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
                return mapper.readTree(response).get("answerId").asLong();
            };
            Future<Long> first = executor.submit(submit);
            Future<Long> second = executor.submit(submit);
            assertThat(first.get(10, TimeUnit.SECONDS))
                    .isEqualTo(second.get(10, TimeUnit.SECONDS));
        }
        org.assertj.core.api.Assertions.assertThat(answers.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(evaluations.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 회원에게는 답변과 평가의 존재를 숨긴다")
    void hidesAnotherMembersAnswer() throws Exception {
        Member owner = members.save(Member.builder().nickname("소유자").build());
        Member stranger = members.save(Member.builder().nickname("다른 회원").build());
        Question question = publishedQuestion();
        String response = mvc.perform(post("/api/questions/{id}/answers", question.getId())
                        .with(user(principal(owner))).with(csrf()).contentType("application/json")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(mapper.writeValueAsString(Map.of("content", "개인 답변"))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).get("answerId").asLong();
        mvc.perform(get("/api/answers/{id}", id).with(user(principal(stranger))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ANSWER_NOT_FOUND"));
        mvc.perform(get("/api/answers/{id}/evaluation", id).with(user(principal(stranger))))
                .andExpect(status().isNotFound());
    }

    private AuthenticatedMember principal(Member member) {
        return AuthenticatedMember.from(
                AuthAccount.builder().member(member).loginId("learner@example.com").passwordHash("hash").build());
    }

    private Question publishedQuestion() {
        Member admin = members.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topics.save(Topic.builder().code("OS").name("운영체제").build());
        Concept concept = concepts.save(Concept.builder().topic(topic).code("THREAD").name("스레드").build());
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content("스레드를 설명하세요").referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.addConcept(concept, BigDecimal.ONE, true);
        question.review(admin);
        question.publish();
        return questions.save(question);
    }
}
