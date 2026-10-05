package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.exception.ProviderRequestRejectedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class OpenAiEvaluationResponseParserTest {

    private final OpenAiEvaluationResponseParser openAiEvaluationResponseParser =
            new OpenAiEvaluationResponseParser(new ObjectMapper(), "gpt-5.6-terra", "os-evaluator-v1");

    @Test
    @DisplayName("내부 평가 JSON이 정확히 byte 한도이면 정상 결과를 반환한다")
    void acceptsNestedEvaluationAtByteLimit() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = (ObjectNode) mapper.readTree(successResponse());
        ObjectNode textPart = (ObjectNode) response.at("/output/0/content/0");
        String outputText = textPart.get("text").stringValue();
        textPart.put("text", outputText + " ".repeat(65_536 - outputText.getBytes(StandardCharsets.UTF_8).length));

        EvaluationResult result = openAiEvaluationResponseParser.parse(mapper.writeValueAsString(response), 1L);

        assertThat(result.verdict()).isEqualTo(Verdict.CORRECT);
    }

    @Test
    @DisplayName("한도를 넘는 평가 output text는 내부 JSON을 파싱하기 전에 영구 거부한다")
    void rejectsOversizedNestedEvaluation() {
        String responseBody = successResponse().replace("정확함", "가".repeat(22_000));
        String outputText = new ObjectMapper().readTree(responseBody).at("/output/0/content/0/text").stringValue();
        ObjectMapper mapper = spy(new ObjectMapper());
        OpenAiEvaluationResponseParser parser = new OpenAiEvaluationResponseParser(mapper, "test-model", "test-version");

        assertThatThrownBy(() -> parser.parse(responseBody, 1L))
                .isInstanceOf(ProviderRequestRejectedException.class)
                .hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
        verify(mapper, never()).readTree(outputText);
    }

    @Test
    @DisplayName("구조화 결과와 token 사용량을 내부 평가 계약으로 변환한다")
    void parsesStructuredEvaluationResult() {
        EvaluationResult evaluationResult = openAiEvaluationResponseParser.parse(successResponse(), 42L);

        assertThat(evaluationResult.verdict()).isEqualTo(Verdict.CORRECT);
        assertThat(evaluationResult.concepts()).extracting(concept -> concept.conceptId()).containsExactly(11L);
        assertThat(evaluationResult.evidenceChunkIds()).containsExactly(21L);
        assertThat(evaluationResult.durationMillis()).isEqualTo(42L);
        assertThat(evaluationResult.inputTokens()).isEqualTo(800L);
        assertThat(evaluationResult.outputTokens()).isEqualTo(200L);
        assertThat(evaluationResult.modelName()).isEqualTo("gpt-5.6-terra");
        assertThat(evaluationResult.evaluatorVersion()).isEqualTo("os-evaluator-v1");
    }

    @Test
    @DisplayName("필수 필드가 빠진 provider 결과를 계약 위반으로 거부한다")
    void rejectsMissingProviderField() {
        String responseBody = """
                {"output":[{"content":[{"type":"output_text","text":"{\\"verdict\\":\\"CORRECT\\"}"}]}],
                 "usage":{"input_tokens":1,"output_tokens":1}}
                """;

        assertThatThrownBy(() -> openAiEvaluationResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    @Test
    @DisplayName("추가 필드가 포함된 provider 결과를 strict 계약 위반으로 거부한다")
    void rejectsAdditionalProviderField() {
        String responseBody = successResponse().replace(
                "\\\"evidenceChunkIds\\\":[21]}",
                "\\\"evidenceChunkIds\\\":[21],\\\"unexpected\\\":true}"
        );

        assertThatThrownBy(() -> openAiEvaluationResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    @Test
    @DisplayName("정수가 아닌 token 사용량을 provider 계약 위반으로 거부한다")
    void rejectsNonIntegerTokenUsage() {
        String responseBody = successResponse().replace("\"input_tokens\": 800", "\"input_tokens\": \"800\"");

        assertThatThrownBy(() -> openAiEvaluationResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    @Test
    @DisplayName("음수 token 사용량을 provider 계약 위반으로 거부한다")
    void rejectsNegativeTokenUsage() {
        String responseBody = successResponse().replace("\"output_tokens\": 200", "\"output_tokens\": -1");

        assertThatThrownBy(() -> openAiEvaluationResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    @Test
    @DisplayName("타입이 다른 평가 필드를 provider 계약 위반으로 거부한다")
    void rejectsWrongEvaluationFieldType() {
        String responseBody = successResponse().replace("\\\"conceptId\\\":11", "\\\"conceptId\\\":\\\"11\\\"");

        assertThatThrownBy(() -> openAiEvaluationResponseParser.parse(responseBody, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    private String successResponse() {
        return """
                {
                  "output": [{"content": [{"type": "output_text", "text": "{\\"verdict\\":\\"CORRECT\\",\\"feedback\\":\\"정확함\\",\\"strengths\\":[\\"자원 소유 설명\\"],\\"omissions\\":[],\\"misconceptions\\":[],\\"concepts\\":[{\\"conceptId\\":11,\\"verdict\\":\\"CORRECT\\",\\"feedback\\":\\"정확함\\"}],\\"evidenceChunkIds\\":[21]}"}]}],
                  "usage": {"input_tokens": 800, "output_tokens": 200}
                }
                """;
    }
}
