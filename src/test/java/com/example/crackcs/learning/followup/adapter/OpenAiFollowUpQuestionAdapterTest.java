package com.example.crackcs.learning.followup.adapter;

import com.example.crackcs.evaluation.adapter.openai.OpenAiResponsesClient;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.learning.followup.domain.FollowUpResult;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiFollowUpQuestionAdapterTest {
    private final ObjectMapper mapper = new ObjectMapper();

    static Stream<String> invalidResults() {
        return Stream.of(
                "{\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":null,\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":null,\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":null,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":null}",
                "{\"content\":\"\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":12,\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":\"11\",\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":12,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[8]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[7,7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[7],\"extra\":true}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":9223372036854775808,\"evidenceChunkIds\":[7]}");
    }

    @Test
    @DisplayName("질문 응답 스키마는 네 필드의 필수 여부와 문자열 및 식별자 범위를 선언한다")
    void declaresQuestionResponseSchema() {
        OpenAiResponsesClient client = (body, timeout) -> {
            JsonNode schema = mapper.readTree(body).at("/text/format/schema");
            JsonNode expected = mapper.valueToTree(Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "required", List.of("content", "referenceAnswer", "conceptId", "evidenceChunkIds"),
                    "properties", Map.of(
                            "content", Map.of("type", "string", "minLength", 1, "maxLength", 10000),
                            "referenceAnswer", Map.of("type", "string", "minLength", 1, "maxLength", 10000),
                            "conceptId", Map.of("type", "integer", "minimum", 1),
                            "evidenceChunkIds", Map.of("type", "array", "minItems", 1,
                                    "items", Map.of("type", "integer", "minimum", 1)))));
            assertThat(schema).isEqualTo(expected);
            return response(valid());
        };

        adapter(client).generate(request());
    }

    @Test
    @DisplayName("판정과 승인 근거를 strict schema로 전달하고 검증된 결과와 사용량을 반환한다")
    void sendsStrictRequestAndParsesResult() {
        OpenAiResponsesClient client = (body, timeout) -> {
            JsonNode sent = mapper.readTree(body);
            assertThat(sent.at("/text/format/strict").booleanValue()).isTrue();
            assertThat(sent.at("/text/format/schema/additionalProperties").booleanValue()).isFalse();
            assertThat(sent.get("store").booleanValue()).isFalse();
            assertThat(body).contains("CORRECT", "근거");
            assertThat(timeout).isEqualTo(Duration.ofSeconds(3));
            return response(valid());
        };
        FollowUpResult result = adapter(client).generate(request());
        assertThat(result.content()).isEqualTo("질문");
        assertThat(result.conceptId()).isEqualTo(11L);
        assertThat(result.evidenceChunkIds()).containsExactly(7L);
        assertThat(result.inputTokens()).isEqualTo(10);
        assertThat(result.generatorVersion()).isEqualTo("follow-up-v1");
    }

    @ParameterizedTest
    @MethodSource("invalidResults")
    @DisplayName("스키마 타입 범위와 승인 개념 또는 근거를 벗어난 출력을 거부한다")
    void rejectsInvalidOutput(String json) {
        assertThatThrownBy(() -> adapter((body, timeout) -> response(mapper.readTree(json))).generate(request()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"output\":null}", "{\"output\":[{}]}",
            "{\"output\":[{\"content\":null}]}",
            "{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":null}]}]}"})
    @DisplayName("외부 응답의 누락된 필드와 명시적 null을 잘못된 생성 결과로 거부한다")
    void rejectsMissingResponseFields(String response) {
        assertThatThrownBy(() -> adapter((body, timeout) -> response).generate(request()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("토큰 사용량의 문자열 강제 변환을 허용하지 않는다")
    void rejectsStringUsage() {
        String response = response(valid()).replace("\"input_tokens\":10", "\"input_tokens\":\"10\"");
        assertThatThrownBy(() -> adapter((body, timeout) -> response).generate(request()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private OpenAiFollowUpQuestionAdapter adapter(OpenAiResponsesClient client) {
        return new OpenAiFollowUpQuestionAdapter(client, mapper, "test-model", Duration.ofSeconds(3));
    }

    private FollowUpRequest request() {
        return new FollowUpRequest("원본 질문", 11L, "개념", Verdict.CORRECT, "피드백", List.of(), List.of(),
                List.of(new FollowUpRequest.Evidence(7L, "근거")));
    }

    private ObjectNode valid() {
        ObjectNode result = mapper.createObjectNode().put("content", "질문").put("referenceAnswer", "정답").put("conceptId", 11);
        result.putArray("evidenceChunkIds").add(7);
        return result;
    }

    private String response(JsonNode result) {
        return mapper.writeValueAsString(Map.of("usage", Map.of("input_tokens", 10, "output_tokens", 5),
                "output", List.of(Map.of("content", List.of(Map.of("type", "output_text", "text", mapper.writeValueAsString(result)))))));
    }
}
