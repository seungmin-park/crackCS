package com.example.crackcs.learning.followup.adapter;

import com.example.crackcs.learning.followup.domain.FollowUpGenerationResult;
import com.example.crackcs.exception.ProviderRequestRejectedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OpenAiFollowUpResponseParserTest {

    private final OpenAiFollowUpResponseParser openAiFollowUpResponseParser =
            new OpenAiFollowUpResponseParser(new ObjectMapper(), "test-model");

    @Test
    @DisplayName("내부 후속 질문 JSON이 정확히 byte 한도이면 정상 결과를 반환한다")
    void acceptsNestedQuestionAtByteLimit() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = (ObjectNode) mapper.readTree(successResponse());
        ObjectNode textPart = (ObjectNode) response.at("/output/0/content/0");
        String outputText = textPart.get("text").stringValue();
        textPart.put("text", outputText + " ".repeat(65_536 - outputText.getBytes(StandardCharsets.UTF_8).length));

        FollowUpGenerationResult result = openAiFollowUpResponseParser.parse(mapper.writeValueAsString(response), 1L);

        assertThat(result.content()).isEqualTo("질문");
    }

    @Test
    @DisplayName("한도를 넘는 후속 질문 output text는 내부 JSON을 파싱하기 전에 영구 거부한다")
    void rejectsOversizedNestedQuestion() {
        String responseBody = successResponse().replace("질문", "가".repeat(22_000));
        String outputText = new ObjectMapper().readTree(responseBody).at("/output/0/content/0/text").stringValue();
        ObjectMapper mapper = spy(new ObjectMapper());
        OpenAiFollowUpResponseParser parser = new OpenAiFollowUpResponseParser(mapper, "test-model");

        assertThatThrownBy(() -> parser.parse(responseBody, 1L))
                .isInstanceOf(ProviderRequestRejectedException.class)
                .hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
        verify(mapper, never()).readTree(outputText);
    }

    static Stream<String> invalidQuestionResults() {
        return Stream.of(
                "{\"content\":\"\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"   \",\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":null,\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":null,\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":12,\"conceptId\":11,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":null,\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":\"11\",\"evidenceChunkIds\":[7]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":null}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[]}",
                "{\"content\":\"질문\",\"referenceAnswer\":\"정답\",\"conceptId\":11,\"evidenceChunkIds\":[7,7]}"
        );
    }

    @Test
    @DisplayName("구조화된 단일 질문과 token 사용량을 생성 결과로 변환한다")
    void parsesStructuredFollowUpResult() {
        FollowUpGenerationResult generationResult = openAiFollowUpResponseParser.parse(successResponse(), 42L);

        assertThat(generationResult.content()).isEqualTo("질문");
        assertThat(generationResult.referenceAnswer()).isEqualTo("정답");
        assertThat(generationResult.conceptId()).isEqualTo(11L);
        assertThat(generationResult.evidenceChunkIds()).containsExactly(7L);
        assertThat(generationResult.modelName()).isEqualTo("test-model");
        assertThat(generationResult.generatorVersion()).isEqualTo("follow-up-v1");
        assertThat(generationResult.durationMillis()).isEqualTo(42L);
        assertThat(generationResult.inputTokens()).isEqualTo(10L);
        assertThat(generationResult.outputTokens()).isEqualTo(5L);
    }

    @Test
    @DisplayName("복수 output text를 하나로 임의 선택하지 않고 거부한다")
    void rejectsMultipleOutputTexts() {
        String responseBody = """
                {
                  "output": [{"content": [
                    {"type": "output_text", "text": "{\\"content\\":\\"첫 질문\\",\\"referenceAnswer\\":\\"정답\\",\\"conceptId\\":11,\\"evidenceChunkIds\\":[7]}"},
                    {"type": "output_text", "text": "{\\"content\\":\\"둘째 질문\\",\\"referenceAnswer\\":\\"정답\\",\\"conceptId\\":11,\\"evidenceChunkIds\\":[7]}"}
                  ]}],
                  "usage": {"input_tokens": 10, "output_tokens": 5}
                }
                """;

        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @Test
    @DisplayName("long 범위를 벗어난 concept ID를 거부한다")
    void rejectsOutOfRangeConceptId() {
        String responseBody = successResponse().replace(
                "\\\"conceptId\\\":11",
                "\\\"conceptId\\\":9223372036854775808"
        );

        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @Test
    @DisplayName("정수가 아닌 evidence ID를 거부한다")
    void rejectsNonIntegerEvidenceId() {
        String responseBody = successResponse().replace(
                "\\\"evidenceChunkIds\\\":[7]",
                "\\\"evidenceChunkIds\\\":[\\\"7\\\"]"
        );

        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @Test
    @DisplayName("필수 필드가 빠진 질문 결과를 거부한다")
    void rejectsMissingResultField() {
        String responseBody = successResponse().replace("\\\"referenceAnswer\\\":\\\"정답\\\",", "");

        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @Test
    @DisplayName("추가 필드가 포함된 질문 결과를 거부한다")
    void rejectsAdditionalResultField() {
        String responseBody = successResponse().replace(
                "\\\"evidenceChunkIds\\\":[7]}",
                "\\\"evidenceChunkIds\\\":[7],\\\"unexpected\\\":true}"
        );

        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @Test
    @DisplayName("문자열 token 사용량을 정수로 강제 변환하지 않고 거부한다")
    void rejectsStringTokenUsage() {
        String responseBody = successResponse().replace("\"input_tokens\": 10", "\"input_tokens\": \"10\"");

        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @Test
    @DisplayName("음수 token 사용량을 거부한다")
    void rejectsNegativeTokenUsage() {
        String responseBody = successResponse().replace("\"output_tokens\": 5", "\"output_tokens\": -1");

        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @ParameterizedTest
    @MethodSource("invalidQuestionResults")
    @DisplayName("질문 필드의 null, 잘못된 타입, 빈 값과 중복 ID를 거부한다")
    void rejectsInvalidQuestionFields(String resultJson) {
        String escapedResultJson = resultJson.replace("\\", "\\\\").replace("\"", "\\\"");
        String responseBody = """
                {
                  "output": [{"content": [{"type": "output_text", "text": "%s"}]}],
                  "usage": {"input_tokens": 10, "output_tokens": 5}
                }
                """.formatted(escapedResultJson);

        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null",
            "{}",
            "{\"output\":null}",
            "{\"output\":[{}]}",
            "{\"output\":[{\"content\":null}]}",
            "{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":null}]}]}"
    })
    @DisplayName("응답의 누락된 output 구조와 명시적 null을 거부한다")
    void rejectsMissingOutputStructure(String responseBody) {
        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result");
    }

    @Test
    @DisplayName("잘못된 JSON을 거부할 때 파싱 실패 원인을 보존한다")
    void preservesJsonParsingFailureCause() {
        assertThatThrownBy(() -> openAiFollowUpResponseParser.parse("invalid-json", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid follow-up provider result")
                .hasCauseInstanceOf(JacksonException.class);
    }

    @Test
    @DisplayName("예상하지 못한 내부 오류는 provider 결과 오류로 변환하지 않는다")
    void propagatesUnexpectedRuntimeFailure() {
        String responseBody = successResponse();
        ObjectMapper failingMapper = spy(new ObjectMapper());
        IllegalStateException failure = new IllegalStateException("unexpected internal failure");
        doThrow(failure).when(failingMapper).readTree(responseBody);
        OpenAiFollowUpResponseParser parser = new OpenAiFollowUpResponseParser(failingMapper, "test-model");

        assertThatThrownBy(() -> parser.parse(responseBody, 1L)).isSameAs(failure);
    }

    private String successResponse() {
        return """
                {
                  "output": [{"content": [{"type": "output_text", "text": "{\\"content\\":\\"질문\\",\\"referenceAnswer\\":\\"정답\\",\\"conceptId\\":11,\\"evidenceChunkIds\\":[7]}"}]}],
                  "usage": {"input_tokens": 10, "output_tokens": 5}
                }
                """;
    }
}
