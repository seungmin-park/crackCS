package com.example.crackcs.learning.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkService;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionConceptRepository;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.adapter.StubEvaluationAdapter;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.port.EvaluationResult;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import com.example.crackcs.learning.controller.response.AnswerResponse;
import com.example.crackcs.learning.repository.AnswerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"crackcs.evaluation.worker-enabled=true", "crackcs.evaluation.poll-delay=20"})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EvaluationWorkerTest {
    @Autowired
    AnswerService service;
    @Autowired
    EvaluationProcessor processor;
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
    AnswerRepository answers;
    @Autowired
    EvaluationRepository evaluations;
    @Autowired
    KnowledgeDocumentRepository knowledgeDocuments;
    @Autowired
    KnowledgeChunkRepository knowledgeChunks;
    @Autowired
    KnowledgeChunkService chunkService;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    CommitSignalPort port;

    @AfterEach
    void tearDown() {
        jdbc.update("delete from evaluation_evidence");
        jdbc.update("delete from evaluation_strength");
        jdbc.update("delete from evaluation_omission");
        jdbc.update("delete from evaluation_misconception");
        jdbc.update("delete from evaluation_concept");
        evaluations.deleteAllInBatch();
        answers.deleteAllInBatch();
        questionConcepts.deleteAllInBatch();
        questions.deleteAllInBatch();
        knowledgeChunks.deleteAllInBatch();
        knowledgeDocuments.deleteAllInBatch();
        concepts.deleteAllInBatch();
        topics.deleteAllInBatch();
        members.deleteAllInBatch();
    }

    @Test
    @DisplayName("별도 알림 없이 저장된 평가 대기를 찾아 자동으로 평가를 확정한다")
    void discoversDurablePendingWork() throws Exception {
        Member member = members.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        AnswerResponse response = service.submit(
                member.getId(),
                question.getId(),
                UUID.randomUUID().toString(),
                "답변"
        );

        awaitEvaluation(response.answerId());

        assertThat(service.findEvaluation(member.getId(), response.answerId()).concepts()).hasSize(1);
        assertThat(port.calledOutsideTransaction).isTrue();
    }

    @TestConfiguration
    static class PortConfiguration {
        @Bean
        @Primary
        CommitSignalPort commitSignalPort() {
            return new CommitSignalPort();
        }
    }

    static class CommitSignalPort implements EvaluationPort {
        volatile boolean calledOutsideTransaction;

        @Override
        public EvaluationResult evaluate(EvaluationRequest request) {
            calledOutsideTransaction = !TransactionSynchronizationManager
                    .isActualTransactionActive();
            return new StubEvaluationAdapter("CORRECT").evaluate(request);
        }
    }

    private void awaitEvaluation(Long answerId) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (evaluations.findByAnswerId(answerId).orElseThrow().getStatus() == EvaluationStatus.EVALUATED) {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(evaluations.findByAnswerId(answerId).orElseThrow().getStatus())
                .isEqualTo(EvaluationStatus.EVALUATED);
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
        Question saved = questions.save(question);
        KnowledgeDocument document = knowledgeDocuments.save(KnowledgeDocument.builder()
                .topic(topic).createdByMember(admin).title("스레드 공개 근거")
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성")
                .content("스레드는 프로세스 자원을 공유하는 실행 단위다.").build());
        document.review(admin);
        document.publish();
        knowledgeDocuments.save(document);
        chunkService.generateChunks(document.getId());
        return saved;
    }
}
