package com.example.crackcs.learning.answer.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkService;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.answer.service.result.AnswerResult;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "crackcs.evaluation.worker-enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@ActiveProfiles("test")
class AnswerQueryCostTest {
    @Autowired
    private AnswerService answerService;

    @Autowired
    private EvaluationProcessor evaluationProcessor;

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
    private KnowledgeChunkService knowledgeChunkService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private AppliedEvaluationConceptRepository appliedEvaluationConceptRepository;

    @Autowired
    private KnowledgeStateRepository knowledgeStateRepository;

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
    @DisplayName("답변 목록은 질문과 평가 및 Concept을 페이지 단위로 일괄 조회한다")
    void loadsAnswerPageWithoutPerAnswerQueries() {
        Member member = memberRepository.save(Member.builder().nickname("학습자").build());
        Question question = publishedQuestion();
        AnswerResult first = answerService.submit(
                member.getId(),
                question.getId(),
                UUID.randomUUID().toString(),
                "첫 답변"
        );
        AnswerResult second = answerService.submit(
                member.getId(),
                question.getId(),
                UUID.randomUUID().toString(),
                "둘째 답변"
        );
        evaluationProcessor.process(first.evaluationId());
        evaluationProcessor.process(second.evaluationId());
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<AnswerResult> page = answerService.findAll(member.getId(), PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().getFirst().evaluation().concepts()).hasSize(1);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(7);
    }

    private Question publishedQuestion() {
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("OS").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("THREAD").name("스레드").build());
        Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                .content("스레드를 설명하세요").referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
        question.replaceConcepts(List.of(
                new QuestionConceptAssignment(concept, BigDecimal.ONE, true)
        ));
        question.review(admin);
        question.publish();
        Question saved = questionRepository.save(question);
        KnowledgeDocument document = knowledgeDocumentRepository.save(KnowledgeDocument.builder()
                .topic(topic)
                .createdByMember(admin)
                .title("스레드 공개 근거")
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general")
                .licenseNote("독립 작성")
                .content("스레드는 프로세스 자원을 공유하는 실행 단위다.")
                .build());
        document.review(admin);
        document.publish();
        knowledgeDocumentRepository.save(document);
        knowledgeChunkService.generateChunks(document.getId());
        return saved;
    }
}
