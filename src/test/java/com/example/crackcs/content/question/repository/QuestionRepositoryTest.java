package com.example.crackcs.content.question.repository;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Test
    @DisplayName("문제를 저장하고 ID로 조회한다")
    void savesAndFindsQuestionById() {
        Topic topic = topicRepository.save(Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build());
        Question question = Question.builder()
                .topic(topic)
                .difficulty(QuestionDifficulty.BASIC)
                .content("프로세스와 스레드의 차이를 설명하세요.")
                .referenceAnswer("프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다.")
                .build();

        Question savedQuestion = questionRepository.save(question);
        Long questionId = savedQuestion.getId();

        Question foundQuestion = questionRepository.findById(questionId).orElseThrow();

        assertThat(foundQuestion.getId()).isEqualTo(questionId);
        assertThat(foundQuestion.getTopicId()).isEqualTo(topic.getId());
        assertThat(foundQuestion.getDifficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(foundQuestion.getContent()).isEqualTo("프로세스와 스레드의 차이를 설명하세요.");
        assertThat(foundQuestion.getReferenceAnswer())
                .isEqualTo("프로세스는 자원을 독립적으로 소유하고, 스레드는 프로세스의 자원을 공유합니다.");
        assertThat(foundQuestion.getCreatedAt()).isNotNull();
        assertThat(foundQuestion.getUpdatedAt()).isNotNull();
    }
}
