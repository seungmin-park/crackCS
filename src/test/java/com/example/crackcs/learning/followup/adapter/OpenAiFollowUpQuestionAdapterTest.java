package com.example.crackcs.learning.followup.adapter;

import com.example.crackcs.evaluation.adapter.openai.OpenAiResponsesClient;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.learning.followup.domain.FollowUpGenerationResult;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiFollowUpQuestionAdapterTest {

    @Test
    @DisplayName("생성한 요청을 제한 시간과 함께 전송하고 파싱한 후속 질문을 반환한다")
    void sendsGeneratedRequestAndReturnsParsedQuestion() {
        ObjectMapper objectMapper = new ObjectMapper();
        RecordingClient openAiResponsesClient = new RecordingClient(successResponse());
        OpenAiFollowUpRequestFactory openAiFollowUpRequestFactory =
                new OpenAiFollowUpRequestFactory(objectMapper, "test-model", "low", 2048);
        OpenAiFollowUpResponseParser openAiFollowUpResponseParser =
                new OpenAiFollowUpResponseParser(objectMapper, "test-model");
        OpenAiFollowUpQuestionAdapter adapter = new OpenAiFollowUpQuestionAdapter(
                openAiResponsesClient,
                openAiFollowUpRequestFactory,
                openAiFollowUpResponseParser,
                Duration.ofSeconds(3)
        );
        FollowUpRequest followUpRequest = new FollowUpRequest(
                "원본 질문", 11L, "개념", Verdict.CORRECT, "피드백", List.of(), List.of(),
                List.of(new FollowUpRequest.Evidence(7L, "근거"))
        );

        FollowUpGenerationResult generationResult = adapter.generate(followUpRequest);

        assertThat(openAiResponsesClient.requestBody).contains("\"model\":\"test-model\"");
        assertThat(openAiResponsesClient.timeout).isEqualTo(Duration.ofSeconds(3));
        assertThat(generationResult.content()).isEqualTo("질문");
        assertThat(generationResult.conceptId()).isEqualTo(11L);
        assertThat(generationResult.evidenceChunkIds()).containsExactly(7L);
    }

    private String successResponse() {
        return """
                {
                  "output": [{"content": [{"type": "output_text", "text": "{\\"content\\":\\"질문\\",\\"referenceAnswer\\":\\"정답\\",\\"conceptId\\":11,\\"evidenceChunkIds\\":[7]}"}]}],
                  "usage": {"input_tokens": 10, "output_tokens": 5}
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
