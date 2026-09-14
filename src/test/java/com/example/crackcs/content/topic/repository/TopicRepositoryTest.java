package com.example.crackcs.content.topic.repository;

import com.example.crackcs.content.topic.domain.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TopicRepositoryTest {

    @Autowired
    private TopicRepository topicRepository;

    @Test
    @DisplayName("최상위 주제를 이름순으로 조회한다")
    void findsRootTopicsByName() {
        topicRepository.save(createTopic("NETWORK", "네트워크"));
        topicRepository.save(createTopic("OPERATING_SYSTEM", "운영체제"));

        List<Topic> topics = topicRepository.findAllByParentIsNullOrderByNameAsc();

        assertThat(topics)
                .extracting(Topic::getName)
                .containsExactly("네트워크", "운영체제");
    }

    @Test
    @DisplayName("상위 주제 ID로 하위 주제를 이름순으로 조회한다")
    void findsChildTopicsByParentId() {
        Topic parent = topicRepository.save(createTopic("COMPUTER_SCIENCE", "컴퓨터 과학"));
        topicRepository.save(createTopic(parent, "NETWORK", "네트워크"));
        topicRepository.save(createTopic(parent, "OPERATING_SYSTEM", "운영체제"));

        List<Topic> topics = topicRepository.findAllByParentIdOrderByNameAsc(parent.getId());

        assertThat(topics)
                .extracting(Topic::getName)
                .containsExactly("네트워크", "운영체제");
    }

    @Test
    @DisplayName("같은 주제 코드를 중복 저장할 수 없다")
    void rejectsDuplicateCode() {
        topicRepository.save(createTopic("OPERATING_SYSTEM", "운영체제"));

        assertThatThrownBy(() -> topicRepository.save(
                createTopic("OPERATING_SYSTEM", "운영체제 심화")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Topic createTopic(String code, String name) {
        return createTopic(null, code, name);
    }

    private Topic createTopic(Topic parent, String code, String name) {
        return Topic.builder()
                .parent(parent)
                .code(code)
                .name(name)
                .build();
    }
}
