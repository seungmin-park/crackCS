package com.example.crackcs.content.question.repository;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("문제 저장소")
class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        questionRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("문제를 저장하고 ID로 조회한다")
    void savesAndFindsQuestionById() {
        Question question = Question.builder()
                .topicId(1L)
                .difficulty(QuestionDifficulty.BASIC)
                .content("프로세스와 스레드의 차이를 설명하세요.")
                .referenceAnswer("프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다.")
                .build();

        Question savedQuestion = questionRepository.saveAndFlush(question);
        Long questionId = savedQuestion.getId();
        entityManager.clear();

        Question foundQuestion = questionRepository.findById(questionId).orElseThrow();

        assertThat(foundQuestion).isNotSameAs(savedQuestion);
        assertThat(foundQuestion.getId()).isEqualTo(questionId);
        assertThat(foundQuestion.getTopicId()).isEqualTo(1L);
        assertThat(foundQuestion.getDifficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(foundQuestion.getContent()).isEqualTo("프로세스와 스레드의 차이를 설명하세요.");
        assertThat(foundQuestion.getReferenceAnswer())
                .isEqualTo("프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다.");
        assertThat(foundQuestion.getCreatedAt()).isNotNull();
        assertThat(foundQuestion.getUpdatedAt()).isNotNull();
    }
}
