package com.example.crackcs.content.question.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PublicQuestionServiceTest {

    @Autowired
    private PublicQuestionService publicQuestionService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private TopicRepository topicRepository;

    private Topic topic;
    private Concept concept;

    @BeforeEach
    void setup() {
        topic = topicRepository.save(Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build());
        concept = conceptRepository.save(Concept.builder()
                .topic(topic)
                .code("PROCESS_THREAD")
                .name("프로세스와 스레드")
                .build());
    }

    @Test
    @DisplayName("공개 문제 목록에서는 PUBLISHED 문제만 조회한다")
    void findsOnlyPublishedQuestions() {
        saveQuestion("공개 질문", QuestionDifficulty.BASIC, true, false);
        saveQuestion("초안 질문", QuestionDifficulty.BASIC, false, false);
        saveQuestion("폐기 질문", QuestionDifficulty.BASIC, true, true);

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
        saveQuestion("기본 질문", QuestionDifficulty.BASIC, true, false);
        saveQuestion("심화 질문", QuestionDifficulty.ADVANCED, true, false);

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
        Question draft = saveQuestion("초안 질문", QuestionDifficulty.BASIC, false, false);

        assertThatThrownBy(() -> publicQuestionService.findById(draft.getId()))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Question not found: " + draft.getId());
    }

    private Question saveQuestion(
            String content,
            QuestionDifficulty difficulty,
            boolean publish,
            boolean retire
    ) {
        Question question = Question.builder()
                .topic(topic)
                .difficulty(difficulty)
                .content(content)
                .referenceAnswer("모범 답안")
                .build();
        if (publish) {
            question.addConcept(concept, BigDecimal.ONE, true);
            question.publish();
        }
        if (retire) {
            question.retire();
        }
        return questionRepository.save(question);
    }
}
