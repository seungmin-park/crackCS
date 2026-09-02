package com.example.crackcs.content.question.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.content.question.repository.QuestionConceptRepository;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class QuestionServiceTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionConceptRepository questionConceptRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @AfterEach
    void tearDown() {
        questionConceptRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("관리자 초안 일반 문제를 생성하고 저장한다")
    void createsAndSavesQuestion() {
        Topic firstTopic = saveTopic("OPERATING_SYSTEM", "운영체제");
        Question createdQuestion = questionService.create(
                saveAdmin().getId(),
                firstTopic.getId(),
                QuestionDifficulty.BASIC,
                "프로세스와 스레드의 차이를 설명하세요.",
                "프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다."
        );

        Question savedQuestion = questionRepository.findById(createdQuestion.getId()).orElseThrow();

        assertThat(savedQuestion.getTopicId()).isEqualTo(firstTopic.getId());
        assertThat(savedQuestion.getOrigin()).isEqualTo(QuestionOrigin.ADMIN);
        assertThat(savedQuestion.getType()).isEqualTo(QuestionType.NORMAL);
        assertThat(savedQuestion.getDifficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(savedQuestion.getStatus()).isEqualTo(QuestionStatus.DRAFT);
        assertThat(savedQuestion.getContent()).isEqualTo("프로세스와 스레드의 차이를 설명하세요.");
        assertThat(savedQuestion.getReferenceAnswer())
                .isEqualTo("프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다.");
    }

    @Test
    @DisplayName("저장된 문제 목록을 조회한다")
    void findsAllQuestions() {
        Topic firstTopic = saveTopic("OPERATING_SYSTEM", "운영체제");
        saveQuestion(firstTopic, "첫 번째 질문");
        saveQuestion(firstTopic, "두 번째 질문");

        Page<Question> questions = questionService.findAll(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(questions.getContent())
                .extracting(Question::getContent)
                .containsExactlyInAnyOrder("첫 번째 질문", "두 번째 질문");
    }

    @Test
    @DisplayName("ID에 해당하는 문제를 조회한다")
    void findsQuestionById() {
        Topic firstTopic = saveTopic("OPERATING_SYSTEM", "운영체제");
        Question savedQuestion = saveQuestion(firstTopic, "기존 질문");
        Long questionId = savedQuestion.getId();

        Question foundQuestion = questionService.findById(questionId);

        assertThat(foundQuestion.getId()).isEqualTo(questionId);
        assertThat(foundQuestion.getContent()).isEqualTo("기존 질문");
    }

    @Test
    @DisplayName("ID에 해당하는 문제가 없으면 예외를 던진다")
    void throwsExceptionWhenQuestionDoesNotExist() {
        assertThatThrownBy(() -> questionService.findById(Long.MAX_VALUE))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Question not found: " + Long.MAX_VALUE);
    }

    @Test
    @DisplayName("문제를 조회한 뒤 정보를 수정하고 저장한다")
    void updatesAndSavesQuestion() {
        Topic firstTopic = saveTopic("OPERATING_SYSTEM", "운영체제");
        Topic secondTopic = saveTopic("NETWORK", "네트워크");
        Question savedQuestion = saveQuestion(firstTopic, "기존 질문");
        Long questionId = savedQuestion.getId();
        LocalDateTime createdAt = savedQuestion.getCreatedAt();
        LocalDateTime firstUpdatedAt = savedQuestion.getUpdatedAt();

        questionService.update(
                questionId,
                secondTopic.getId(),
                QuestionDifficulty.ADVANCED,
                "변경된 질문",
                "변경된 모범 답안"
        );

        Question updatedQuestion = questionService.findById(questionId);

        assertThat(updatedQuestion.getTopicId()).isEqualTo(secondTopic.getId());
        assertThat(updatedQuestion.getDifficulty()).isEqualTo(QuestionDifficulty.ADVANCED);
        assertThat(updatedQuestion.getContent()).isEqualTo("변경된 질문");
        assertThat(updatedQuestion.getReferenceAnswer()).isEqualTo("변경된 모범 답안");
        assertThat(updatedQuestion.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updatedQuestion.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
    }

    @Test
    @DisplayName("수정할 문제가 없으면 예외를 던진다")
    void throwsExceptionWhenQuestionToUpdateDoesNotExist() {
        assertThatThrownBy(() -> questionService.update(
                Long.MAX_VALUE,
                1L,
                QuestionDifficulty.ADVANCED,
                "변경된 질문",
                "변경된 모범 답안"
        ))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Question not found: " + Long.MAX_VALUE);
    }

    @Test
    @DisplayName("평가 Concept을 교체하고 검수한 문제를 공개한다")
    void replacesConceptsReviewsAndPublishesQuestion() {
        Topic topic = saveTopic("OPERATING_SYSTEM", "운영체제");
        Member admin = saveAdmin();
        Concept concept = conceptRepository.save(Concept.builder()
                .topic(topic).code("PROCESS_THREAD").name("프로세스와 스레드").build());
        Question question = questionService.create(
                admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "질문", "모범 답안"
        );

        questionService.replaceConcepts(
                question.getId(), List.of(new QuestionConceptData(concept.getId(), BigDecimal.ONE, true))
        );
        questionService.review(question.getId(), admin.getId());
        Question published = questionService.publish(question.getId());

        assertThat(published.getStatus()).isEqualTo(QuestionStatus.PUBLISHED);
        assertThat(published.getReviewedAt()).isNotNull();
        assertThat(published.getQuestionConcepts()).hasSize(1);
    }

    @Test
    @DisplayName("비활성 Concept을 문제의 평가 기준으로 연결할 수 없다")
    void rejectsInactiveConceptConnection() {
        Topic topic = saveTopic("OPERATING_SYSTEM", "운영체제");
        Member admin = saveAdmin();
        Concept concept = conceptRepository.save(Concept.builder()
                .topic(topic).code("PROCESS_THREAD").name("프로세스와 스레드").build());
        concept.deactivate();
        conceptRepository.save(concept);
        Question question = questionService.create(
                admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "질문", "모범 답안"
        );

        assertThatThrownBy(() -> questionService.replaceConcepts(
                question.getId(), List.of(new QuestionConceptData(concept.getId(), BigDecimal.ONE, true))
        )).isInstanceOf(InvalidContentStateException.class);
    }

    @Test
    @DisplayName("비활성 Topic에는 새 문제를 연결할 수 없다")
    void rejectsInactiveTopicConnection() {
        Topic topic = saveTopic("OPERATING_SYSTEM", "운영체제");
        Member admin = saveAdmin();
        topic.deactivate();
        topicRepository.save(topic);

        assertThatThrownBy(() -> questionService.create(
                admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "질문", "모범 답안"
        ))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("비활성 Topic에는 Question을 연결할 수 없습니다.");
    }

    @Test
    @DisplayName("새 문제 버전을 공개하면 이전 공개본은 보존된 RETIRED 상태가 된다")
    void retiresPreviousVersionWhenPublishingNextVersion() {
        Topic topic = saveTopic("OPERATING_SYSTEM", "운영체제");
        Member admin = saveAdmin();
        Concept concept = conceptRepository.save(Concept.builder()
                .topic(topic).code("PROCESS_THREAD").name("프로세스와 스레드").build());
        Question first = questionService.create(
                admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "첫 문제", "첫 답안"
        );
        questionService.replaceConcepts(
                first.getId(), List.of(new QuestionConceptData(concept.getId(), BigDecimal.ONE, true))
        );
        questionService.review(first.getId(), admin.getId());
        questionService.publish(first.getId());

        Question second = questionService.createNextVersion(
                first.getId(), admin.getId(), QuestionDifficulty.INTERMEDIATE, "둘째 문제", "둘째 답안"
        );
        questionService.review(second.getId(), admin.getId());
        questionService.publish(second.getId());

        assertThat(questionRepository.findById(first.getId()).orElseThrow().getStatus())
                .isEqualTo(QuestionStatus.RETIRED);
        assertThat(questionRepository.findById(second.getId()).orElseThrow().getStatus())
                .isEqualTo(QuestionStatus.PUBLISHED);
        assertThat(questionRepository.findPublishedById(first.getId())).isEmpty();
        assertThat(questionRepository.findPublishedById(second.getId())).isPresent();
    }

    private Topic saveTopic(String code, String name) {
        return topicRepository.save(Topic.builder()
                .code(code)
                .name(name)
                .build());
    }

    private Question saveQuestion(Topic topic, String content) {
        Question question = Question.builder()
                .topic(topic)
                .createdByMember(saveAdmin())
                .difficulty(QuestionDifficulty.BASIC)
                .content(content)
                .referenceAnswer("모범 답안")
                .build();

        return questionRepository.save(question);
    }

    private Member saveAdmin() {
        return memberRepository.save(Member.builder()
                .nickname("관리자-" + System.nanoTime())
                .role(MemberRole.ADMIN)
                .build());
    }
}
