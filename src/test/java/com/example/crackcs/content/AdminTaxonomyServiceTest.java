package com.example.crackcs.content;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.concept.service.ConceptService;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.content.topic.service.TopicService;
import com.example.crackcs.exception.DuplicateContentCodeException;
import com.example.crackcs.exception.InvalidContentStateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AdminTaxonomyServiceTest {

    @Autowired
    TopicService topicService;
    @Autowired
    ConceptService conceptService;
    @Autowired
    ConceptRepository conceptRepository;
    @Autowired
    TopicRepository topicRepository;

    @AfterEach
    void tearDown() {
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Topic을 계층으로 등록하고 간접 순환으로 변경하는 요청을 차단한다")
    void rejectsIndirectTopicCycle() {
        Topic root = topicService.create(null, "CS", "컴퓨터 과학");
        Topic child = topicService.create(root.getId(), "OS", "운영체제");

        assertThatThrownBy(() -> topicService.update(root.getId(), child.getId(), "CS", "컴퓨터 과학"))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("Topic 계층에 순환을 만들 수 없습니다.");
    }

    @Test
    @DisplayName("중복 Topic 코드를 애플리케이션 계층에서 차단한다")
    void rejectsDuplicateTopicCode() {
        topicService.create(null, "OS", "운영체제");
        topicService.create(null, "NETWORK", "네트워크");

        assertThatThrownBy(() -> topicService.create(null, "OS", "다른 운영체제"))
                .isInstanceOf(DuplicateContentCodeException.class);
    }

    @Test
    @DisplayName("중복 Concept 코드를 애플리케이션 계층에서 차단한다")
    void rejectsDuplicateConceptCode() {
        Topic topic = topicService.create(null, "OS", "운영체제");
        conceptService.create(topic.getId(), "THREAD", "스레드", null);

        assertThatThrownBy(() -> conceptService.create(topic.getId(), "THREAD", "다른 스레드", null))
                .isInstanceOf(DuplicateContentCodeException.class);
    }

    @Test
    @DisplayName("비활성 Topic에는 새 Concept을 연결할 수 없고 기존 Topic은 보존한다")
    void blocksNewConnectionToInactiveTopic() {
        Topic topic = topicService.create(null, "OS", "운영체제");
        topicService.deactivate(topic.getId());

        assertThatThrownBy(() -> conceptService.create(topic.getId(), "THREAD", "스레드", null))
                .isInstanceOf(InvalidContentStateException.class);
        assertThat(topicService.findById(topic.getId()).isActive()).isFalse();
    }

    @Test
    @DisplayName("Concept 정보를 수정한다")
    void updatesConcept() {
        Topic topic = topicService.create(null, "OS", "운영체제");
        Concept concept = conceptService.create(topic.getId(), "THREAD", "스레드", "기존 설명");

        conceptService.update(concept.getId(), topic.getId(), "THREAD", "프로세스와 스레드", "변경 설명");

        Concept found = conceptService.findById(concept.getId());
        assertThat(found.getName()).isEqualTo("프로세스와 스레드");
        assertThat(found.getDescription()).isEqualTo("변경 설명");
    }

    @Test
    @DisplayName("Concept을 물리 삭제하지 않고 비활성화한다")
    void deactivatesConcept() {
        Topic topic = topicService.create(null, "OS", "운영체제");
        Concept concept = conceptService.create(topic.getId(), "THREAD", "스레드", "기존 설명");

        conceptService.deactivate(concept.getId());

        Concept found = conceptService.findById(concept.getId());
        assertThat(found.isActive()).isFalse();
    }
}
