package com.example.crackcs.content.question.controller.response;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.topic.domain.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PublicQuestionResponseTest {

    @Test
    @DisplayName("공개 응답에는 Topic과 난이도 및 문제 본문만 포함한다")
    void containsOnlyPublicQuestionFields() {
        Topic topic = Topic.builder()
                .code("OPERATING_SYSTEM")
                .name("운영체제")
                .build();
        Question question = Question.builder()
                .topic(topic)
                .difficulty(QuestionDifficulty.BASIC)
                .content("프로세스와 스레드의 차이를 설명하세요.")
                .referenceAnswer("외부에 노출하면 안 되는 모범 답안")
                .build();

        PublicQuestionResponse response = PublicQuestionResponse.from(question);

        assertThat(response.topic().code()).isEqualTo("OPERATING_SYSTEM");
        assertThat(response.difficulty()).isEqualTo(QuestionDifficulty.BASIC);
        assertThat(response.content()).isEqualTo("프로세스와 스레드의 차이를 설명하세요.");
        assertThat(Arrays.stream(PublicQuestionResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
                .containsExactly("id", "topic", "difficulty", "content")
                .doesNotContain("referenceAnswer", "questionConcepts", "weight");
    }
}
