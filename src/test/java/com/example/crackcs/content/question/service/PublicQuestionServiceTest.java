package com.example.crackcs.content.question.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionConceptRepository;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PublicQuestionServiceTest {

    @Autowired
    private PublicQuestionService publicQuestionService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionConceptRepository questionConceptRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        questionConceptRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("공개 문제 목록에서는 PUBLISHED 문제만 조회한다")
    void findsOnlyPublishedQuestions() {
        Topic topic = saveTopic();
        Concept concept = saveConcept(topic);
        Member admin = saveAdmin();

        Question published = createQuestion(topic, admin, "공개 질문", QuestionDifficulty.BASIC);
        published.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
        published.review(admin);
        published.publish();

        Question draft = createQuestion(topic, admin, "초안 질문", QuestionDifficulty.BASIC);

        Question retired = createQuestion(topic, admin, "폐기 질문", QuestionDifficulty.BASIC);
        retired.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
        retired.review(admin);
        retired.publish();
        retired.retire();
        questionRepository.saveAll(List.of(published, draft, retired));

        Page<Question> questions = publicQuestionService.findAll(
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(questions.getContent())
                .extracting(Question::getContent)
                .containsExactly("공개 질문");
    }

    @Test
    @DisplayName("Topic과 난이도로 공개 문제 목록을 필터링한다")
    void filtersPublishedQuestions() {
        Topic topic = saveTopic();
        Concept concept = saveConcept(topic);
        Member admin = saveAdmin();

        Question basic = createQuestion(topic, admin, "기본 질문", QuestionDifficulty.BASIC);
        basic.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
        basic.review(admin);
        basic.publish();

        Question advanced = createQuestion(topic, admin, "심화 질문", QuestionDifficulty.ADVANCED);
        advanced.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
        advanced.review(admin);
        advanced.publish();
        questionRepository.saveAll(List.of(basic, advanced));

        Page<Question> questions = publicQuestionService.findAll(
                topic.getId(),
                QuestionDifficulty.ADVANCED,
                PageRequest.of(0, 1)
        );

        assertThat(questions.getContent())
                .extracting(Question::getContent)
                .containsExactly("심화 질문");
        assertThat(questions.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("비공개 문제 상세는 존재하지 않는 문제와 같은 예외를 던진다")
    void hidesNonPublishedQuestion() {
        Topic topic = saveTopic();
        Member admin = saveAdmin();
        Question draft = questionRepository.save(
                createQuestion(topic, admin, "초안 질문", QuestionDifficulty.BASIC));

        assertThatThrownBy(() -> publicQuestionService.findById(draft.getId()))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Question not found: " + draft.getId());
    }

    private Topic saveTopic() {
        return topicRepository.save(Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build());
    }

    private Concept saveConcept(Topic topic) {
        return conceptRepository.save(Concept.builder()
                .topic(topic)
                .code("PROCESS_THREAD")
                .name("프로세스와 스레드")
                .build());
    }

    private Member saveAdmin() {
        return memberRepository.save(Member.builder()
                .nickname("관리자")
                .role(MemberRole.ADMIN)
                .build());
    }

    private Question createQuestion(
            Topic topic,
            Member admin,
            String content,
            QuestionDifficulty difficulty
    ) {
        return Question.builder()
                .topic(topic)
                .createdByMember(admin)
                .difficulty(difficulty)
                .content(content)
                .referenceAnswer("모범 답안")
                .build();
    }
}
