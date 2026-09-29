package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.port.EvaluationConceptInput;
import com.example.crackcs.evaluation.port.EvaluationEvidenceInput;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiEvaluationAdapterTest {

    @Test
    @DisplayName("생성한 요청을 제한 시간과 함께 전송하고 파싱한 평가 결과를 반환한다")
    void sendsGeneratedRequestAndReturnsParsedEvaluation() {
        ObjectMapper objectMapper = new ObjectMapper();
        RecordingClient openAiResponsesClient = new RecordingClient(successResponse());
        OpenAiEvaluationRequestFactory openAiEvaluationRequestFactory =
                new OpenAiEvaluationRequestFactory(objectMapper, "gpt-5.6-terra", "low", 2048);
        OpenAiEvaluationResponseParser openAiEvaluationResponseParser =
                new OpenAiEvaluationResponseParser(objectMapper, "gpt-5.6-terra", "os-evaluator-v1");
        OpenAiEvaluationAdapter adapter = new OpenAiEvaluationAdapter(
                openAiResponsesClient, openAiEvaluationRequestFactory,
                openAiEvaluationResponseParser, Duration.ofSeconds(30)
        );

        EvaluationResult evaluationResult = adapter.evaluate(request("프로세스는 자원을 소유한다."));

        assertThat(openAiResponsesClient.requestBody).contains("\"model\":\"gpt-5.6-terra\"");
        assertThat(openAiResponsesClient.timeout).isEqualTo(Duration.ofSeconds(30));
        assertThat(evaluationResult.verdict()).isEqualTo(Verdict.CORRECT);
        assertThat(evaluationResult.modelName()).isEqualTo("gpt-5.6-terra");
    }

    private EvaluationRequest request(String answer) {
        return new EvaluationRequest(
                1L, "프로세스를 설명하세요.", "프로세스는 자원을 소유한다.", answer,
                List.of(new EvaluationConceptInput(11L, "프로세스", true)),
                List.of(new EvaluationEvidenceInput(
                        21L, 31L, "프로세스", 1, 0, 18,
                        "프로세스는 자원을 소유한다.", 5.0
                ))
        );
    }

    private String successResponse() {
        return """
                {
                  "output": [{"content": [{"type": "output_text", "text": "{\\"verdict\\":\\"CORRECT\\",\\"feedback\\":\\"정확함\\",\\"strengths\\":[\\"자원 소유 설명\\"],\\"omissions\\":[],\\"misconceptions\\":[],\\"concepts\\":[{\\"conceptId\\":11,\\"verdict\\":\\"CORRECT\\",\\"feedback\\":\\"정확함\\"}],\\"evidenceChunkIds\\":[21]}"}]}],
                  "usage": {"input_tokens": 800, "output_tokens": 200}
                }
                """;
    }

    private static class RecordingClient implements OpenAiResponsesClient {
        private final String responseBody;
        private String requestBody;
        private Duration timeout;

        private RecordingClient(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public String createResponse(String requestBody, Duration timeout) {
            this.requestBody = requestBody;
            this.timeout = timeout;
            return responseBody;
        }
    }
}
