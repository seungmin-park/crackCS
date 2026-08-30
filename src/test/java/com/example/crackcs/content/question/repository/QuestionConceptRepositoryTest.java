package com.example.crackcs.content.question.repository;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConcept;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class QuestionConceptRepositoryTest {

    @Autowired
    private QuestionConceptRepository questionConceptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("별도 ID로 문제 평가 개념의 가중치와 필수 여부를 저장한다")
    void savesQuestionConceptWithSurrogateKey() {
        Topic topic = topicRepository.save(Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build());
        Concept concept = conceptRepository.save(Concept.builder()
                .topic(topic)
                .code("PROCESS_THREAD")
                .name("프로세스와 스레드")
                .build());
        Question question = Question.builder()
                .topic(topic)
                .difficulty(QuestionDifficulty.BASIC)
                .content("프로세스와 스레드의 차이를 설명하세요.")
                .referenceAnswer("모범 답안")
                .build();
        question.addConcept(concept, new BigDecimal("0.75"), true);
        Question savedQuestion = questionRepository.save(question);
        entityManager.clear();

        List<QuestionConcept> mappings = questionConceptRepository.findAllByQuestionId(savedQuestion.getId());

        assertThat(mappings).hasSize(1);
        assertThat(mappings.getFirst().getId()).isNotNull();
        assertThat(mappings.getFirst().getConcept().getId()).isEqualTo(concept.getId());
        assertThat(mappings.getFirst().getWeight()).isEqualByComparingTo("0.75");
        assertThat(mappings.getFirst().isRequired()).isTrue();
    }

    @Test
    @DisplayName("같은 문제와 개념 조합을 중복 저장할 수 없다")
    void rejectsDuplicatedQuestionAndConcept() {
        Topic topic = topicRepository.save(Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build());
        Concept concept = conceptRepository.save(Concept.builder()
                .topic(topic)
                .code("PROCESS_THREAD")
                .name("프로세스와 스레드")
                .build());
        Question question = Question.builder()
                .topic(topic)
                .difficulty(QuestionDifficulty.BASIC)
                .content("프로세스와 스레드의 차이를 설명하세요.")
                .referenceAnswer("모범 답안")
                .build();
        question.addConcept(concept, new BigDecimal("0.75"), true);
        Question savedQuestion = questionRepository.save(question);

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                        INSERT INTO question_concept (question_id, concept_id, weight, is_required)
                        VALUES (:questionId, :conceptId, 0.25, FALSE)
                        """)
                .setParameter("questionId", savedQuestion.getId())
                .setParameter("conceptId", concept.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }
}
