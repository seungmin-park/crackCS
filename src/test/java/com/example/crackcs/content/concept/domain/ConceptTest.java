package com.example.crackcs.content.concept.domain;

import com.example.crackcs.content.topic.domain.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConceptTest {

    @Test
    @DisplayName("주제와 코드, 이름, 설명으로 개념을 생성한다")
    void createsConcept() {
        Topic topic = createTopic();

        Concept concept = Concept.builder()
                .topic(topic)
                .code("PROCESS_THREAD")
                .name("프로세스와 스레드")
                .description("실행 단위와 자원 공유 차이")
                .build();

        assertThat(concept.getTopic()).isSameAs(topic);
        assertThat(concept.getCode()).isEqualTo("PROCESS_THREAD");
        assertThat(concept.getName()).isEqualTo("프로세스와 스레드");
        assertThat(concept.getDescription()).isEqualTo("실행 단위와 자원 공유 차이");
    }

    @Test
    @DisplayName("존재할 주제가 없으면 개념을 생성할 수 없다")
    void rejectsNullTopic() {
        assertThatThrownBy(() -> Concept.builder()
                .topic(null)
                .code("PROCESS_THREAD")
                .name("프로세스와 스레드")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic must not be null");
    }

    @Test
    @DisplayName("개념 코드는 공백일 수 없다")
    void rejectsBlankCode() {
        assertThatThrownBy(() -> Concept.builder()
                .topic(createTopic())
                .code(" ")
                .name("프로세스와 스레드")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must not be blank");
    }

    @Test
    @DisplayName("개념 이름은 공백일 수 없다")
    void rejectsBlankName() {
        assertThatThrownBy(() -> Concept.builder()
                .topic(createTopic())
                .code("PROCESS_THREAD")
                .name(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    @DisplayName("공백 설명은 없는 설명으로 정규화한다")
    void normalizesBlankDescription() {
        Concept concept = Concept.builder()
                .topic(createTopic())
                .code("PROCESS_THREAD")
                .name("프로세스와 스레드")
                .description(" ")
                .build();

        assertThat(concept.getDescription()).isNull();
    }

    private Topic createTopic() {
        return Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build();
    }
}
