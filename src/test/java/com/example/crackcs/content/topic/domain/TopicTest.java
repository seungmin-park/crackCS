package com.example.crackcs.content.topic.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopicTest {

    @Test
    @DisplayName("상위 주제 없이 코드와 이름으로 주제를 생성한다")
    void createsRootTopic() {
        Topic topic = Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build();

        assertThat(topic.getParent()).isNull();
        assertThat(topic.getCode()).isEqualTo("OPERATING_SYSTEM");
        assertThat(topic.getName()).isEqualTo("운영체제");
    }

    @Test
    @DisplayName("하위 주제는 상위 주제를 참조한다")
    void createsChildTopic() {
        Topic parent = createTopic("COMPUTER_SCIENCE", "컴퓨터 과학");

        Topic child = Topic.builder()
                .parent(parent)
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build();

        assertThat(child.getParent()).isSameAs(parent);
    }

    @Test
    @DisplayName("주제는 자기 자신을 상위 주제로 지정할 수 없다")
    void rejectsSelfAsParent() {
        Topic topic = createTopic("OPERATING_SYSTEM", "운영체제");

        assertThatThrownBy(() -> topic.updateParent(topic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parent must not be self");
    }

    @Test
    @DisplayName("주제 코드는 공백일 수 없다")
    void rejectsBlankCode() {
        assertThatThrownBy(() -> Topic.builder()
                .code(" ")
                .name("운영체제")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
    }

    @Test
    @DisplayName("주제 이름은 공백일 수 없다")
    void rejectsBlankName() {
        assertThatThrownBy(() -> Topic.builder()
                .code("OPERATING_SYSTEM")
                .name(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    private Topic createTopic(String code, String name) {
        return Topic.builder()
                .code(code)
                .name(name)
                .build();
    }
}
