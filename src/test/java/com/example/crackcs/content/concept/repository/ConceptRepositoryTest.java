package com.example.crackcs.content.concept.repository;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class ConceptRepositoryTest {

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Test
    @DisplayName("주제 ID로 개념을 이름순으로 조회한다")
    void findsConceptsByTopicId() {
        Topic operatingSystem = topicRepository.save(createTopic("OPERATING_SYSTEM", "운영체제"));
        Topic network = topicRepository.save(createTopic("NETWORK", "네트워크"));
        conceptRepository.save(createConcept(operatingSystem, "THREAD", "스레드"));
        conceptRepository.save(createConcept(operatingSystem, "PROCESS", "프로세스"));
        conceptRepository.save(createConcept(network, "TCP", "TCP"));

        List<Concept> concepts = conceptRepository.findAllByTopicIdOrderByNameAsc(operatingSystem.getId());

        assertThat(concepts)
                .extracting(Concept::getName)
                .containsExactly("스레드", "프로세스");
    }

    @Test
    @DisplayName("같은 개념 코드를 중복 저장할 수 없다")
    void rejectsDuplicateCode() {
        Topic topic = topicRepository.save(createTopic("OPERATING_SYSTEM", "운영체제"));
        conceptRepository.save(createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드"));

        assertThatThrownBy(() -> conceptRepository.save(
                createConcept(topic, "PROCESS_THREAD", "프로세스와 스레드 심화")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 주제에는 개념을 저장할 수 없다")
    void rejectsUnknownTopic() {
        Topic unknownTopic = topicRepository.getReferenceById(Long.MAX_VALUE);

        assertThatThrownBy(() -> conceptRepository.save(
                createConcept(unknownTopic, "PROCESS_THREAD", "프로세스와 스레드")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Topic createTopic(String code, String name) {
        return Topic.builder()
                .code(code)
                .name(name)
                .build();
    }

    private Concept createConcept(Topic topic, String code, String name) {
        return Concept.builder()
                .topic(topic)
                .code(code)
                .name(name)
                .description(name + " 설명")
                .build();
    }
}
