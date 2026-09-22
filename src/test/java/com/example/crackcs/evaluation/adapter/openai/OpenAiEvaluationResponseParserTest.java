package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiEvaluationResponseParserTest {

    private final OpenAiEvaluationResponseParser openAiEvaluationResponseParser =
            new OpenAiEvaluationResponseParser(new ObjectMapper(), "gpt-5.6-terra", "os-evaluator-v1");

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
