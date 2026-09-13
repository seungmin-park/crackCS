package com.example.crackcs.learning.progress.service;

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
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.progress.service.result.LearningProgressResult;
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
class LearningProgressServiceTest {
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
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private LearningProgressService progressService;

    @AfterEach
    void cleanUp() {
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
    @DisplayName("진도는 회원의 전체 풀이 수와 최근 7일 수 및 최신 다섯 답변의 현재 평가 상태를 반환한다")
    void returnsRecentProgressWithCurrentEvaluationStatus() {
        Fixture f = fixture();
        Long old = completed(f, Verdict.INCORRECT);
        // submittedAt은 생성 시 확정되는 불변 값이므로 과거 풀이 이력의 시간 경계만 SQL로 준비한다.
        jdbc.update("update answer set submitted_at = ? where id = ?", LocalDateTime.now().minusDays(8),
                evaluations.findById(old).orElseThrow().getAnswer().getId());
        for (int i = 0; i < 4; i++) {
            pending(f);
        }
        Long latest = completed(f, Verdict.CORRECT);
        Member other = members.save(Member.builder().nickname("다른 학습자").build());
        pending(other, f.question());
        LearningProgressResult progress = progressService.progress(f.member().getId());
        assertThat(progress.totalAnswers()).isEqualTo(6);
        assertThat(progress.recentAnswerCount()).isEqualTo(5);
        assertThat(progress.recentEvaluations()).hasSize(5);
        assertThat(progress.recentEvaluations().getFirst().answerId()).isEqualTo(
                evaluations.findById(latest).orElseThrow().getAnswer().getId());
        assertThat(progress.recentEvaluations().getFirst().status()).isEqualTo(EvaluationStatus.EVALUATED);
        assertThat(progress.recentEvaluations().getFirst().score()).isEqualTo(100);
        assertThat(progress.recentEvaluations().getLast().status()).isEqualTo(EvaluationStatus.EVALUATING);
        assertThat(progress.topics()).hasSize(1);
        assertThat(progress.recommendation().questionId()).isEqualTo(f.question().getId());
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
