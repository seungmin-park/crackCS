package com.example.crackcs.learning.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.*;
import com.example.crackcs.content.question.repository.*;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.member.domain.*;
import com.example.crackcs.member.repository.MemberRepository;
import com.example.crackcs.learning.repository.AnswerRepository;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.evaluation.domain.*;
import com.example.crackcs.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"crackcs.evaluation.worker-enabled=true", "crackcs.evaluation.poll-delay=20"})
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class EvaluationWorkerTest {
    @Autowired AnswerService service;
    @Autowired EvaluationProcessor processor;
    @Autowired MemberRepository members;
    @Autowired TopicRepository topics;
    @Autowired ConceptRepository concepts;
    @Autowired QuestionRepository questions;
    @Autowired QuestionConceptRepository questionConcepts;
    @Autowired AnswerRepository answers;
    @Autowired EvaluationRepository evaluations;
    @Autowired JdbcTemplate jdbc;
    @Autowired CommitSignalPort port;

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
    @DisplayName("별도 알림 없이 저장된 평가 대기를 찾아 자동으로 평가를 확정한다")
    void discoversDurablePendingWork() throws Exception {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        var response = service.submit(member.getId(), question.getId(), UUID.randomUUID().toString(), "답변");

        assertThat(port.committed.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        assertThat(evaluations.findByAnswerId(response.answerId()).orElseThrow().getStatus())
                .isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(service.findEvaluation(member.getId(), response.answerId()).concepts()).hasSize(1);
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class PortConfiguration {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        CommitSignalPort commitSignalPort() { return new CommitSignalPort(); }
    }

    static class CommitSignalPort implements com.example.crackcs.evaluation.port.EvaluationPort {
        final java.util.concurrent.CountDownLatch committed = new java.util.concurrent.CountDownLatch(1);
        @Override
        public com.example.crackcs.evaluation.port.EvaluationResult evaluate(com.example.crackcs.evaluation.port.EvaluationRequest request) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { committed.countDown(); }
                    });
            return new com.example.crackcs.evaluation.adapter.StubEvaluationAdapter("CORRECT").evaluate(request);
        }
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
