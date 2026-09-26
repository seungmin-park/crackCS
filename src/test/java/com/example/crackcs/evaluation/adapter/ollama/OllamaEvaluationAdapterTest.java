package com.example.crackcs.evaluation.adapter.ollama;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.port.EvaluationConceptInput;
import com.example.crackcs.evaluation.port.EvaluationEvidenceInput;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaEvaluationAdapterTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"low", "medium"})
    @DisplayName("설정한 추론 수준과 JSON 스키마로 평가하고 판정과 토큰 수를 반환한다")
    void evaluatesThroughLocalChatApi(String reasoningEffort) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<String> receivedBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = successResponse().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat");
        OllamaEvaluationAdapter adapter = new OllamaEvaluationAdapter(
                objectMapper, endpoint, "gpt-oss:20b", "os-evaluator-v1", reasoningEffort, Duration.ofSeconds(30)
        );
        EvaluationRequest request = new EvaluationRequest(
                1L, "프로세스를 설명하세요.", "프로세스는 자원을 소유한다.", "프로세스는 자원을 소유합니다.",
                List.of(new EvaluationConceptInput(11L, "자원 소유", true)),
                List.of(new EvaluationEvidenceInput(21L, 31L, "프로세스", 1, 0, 18,
                        "프로세스는 자원을 소유한다.", 5.0))
        );

        EvaluationResult result = adapter.evaluate(request);

        JsonNode sent = objectMapper.readTree(receivedBody.get());
        assertThat(sent.path("model").stringValue()).isEqualTo("gpt-oss:20b");
        assertThat(sent.path("stream").booleanValue()).isFalse();
        assertThat(sent.has("think")).isTrue();
        assertThat(sent.get("think").stringValue()).isEqualTo(reasoningEffort);
        assertThat(sent.path("format").path("required").toString()).contains("verdict", "evidenceChunkIds");
        assertThat(sent.path("messages").get(1).path("content").stringValue()).contains("프로세스는 자원을 소유합니다.");
        assertThat(result.verdict()).isEqualTo(Verdict.CORRECT);
        assertThat(result.evidenceChunkIds()).containsExactly(21L);
        assertThat(result.modelName()).isEqualTo("gpt-oss:20b");
        assertThat(result.evaluatorVersion()).isEqualTo("os-evaluator-v1-" + reasoningEffort);
        assertThat(result.inputTokens()).isEqualTo(800);
        assertThat(result.outputTokens()).isEqualTo(200);
    }

    @Test
    @DisplayName("완료되지 않은 로컬 모델 응답은 평가 결과로 저장하지 않는다")
    void rejectsIncompleteResponse() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            byte[] body = successResponse().replace("\"done\": true", "\"done\": false")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat");
        OllamaEvaluationAdapter adapter = new OllamaEvaluationAdapter(
                new ObjectMapper(), endpoint, "gpt-oss:20b", "os-evaluator-v1", "low", Duration.ofSeconds(30)
        );
        EvaluationRequest request = new EvaluationRequest(
                1L, "프로세스를 설명하세요.", "자원을 소유한다.", "자원을 소유합니다.",
                List.of(new EvaluationConceptInput(11L, "자원 소유", true)), List.of()
        );

        assertThatThrownBy(() -> adapter.evaluate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    @Test
    @DisplayName("토큰 사용량이 없는 로컬 모델 응답은 유효한 평가로 인정하지 않는다")
    void rejectsResponseWithoutUsage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            byte[] body = successResponse().replace("\"prompt_eval_count\": 800,", "")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat");
        OllamaEvaluationAdapter adapter = new OllamaEvaluationAdapter(
                new ObjectMapper(), endpoint, "gpt-oss:20b", "os-evaluator-v1", "low", Duration.ofSeconds(30)
        );
        EvaluationRequest request = new EvaluationRequest(
                1L, "프로세스를 설명하세요.", "자원을 소유한다.", "자원을 소유합니다.",
                List.of(new EvaluationConceptInput(11L, "자원 소유", true)), List.of()
        );

        assertThatThrownBy(() -> adapter.evaluate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    @Test
    @DisplayName("깨진 JSON 응답은 평가 결과로 사용하지 않는다")
    void rejectsMalformedResponse() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            byte[] body = "{".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat");
        OllamaEvaluationAdapter adapter = new OllamaEvaluationAdapter(
                new ObjectMapper(), endpoint, "gpt-oss:20b", "os-evaluator-v1", "low", Duration.ofSeconds(30)
        );
        EvaluationRequest request = new EvaluationRequest(
                1L, "프로세스를 설명하세요.", "자원을 소유한다.", "자원을 소유합니다.",
                List.of(new EvaluationConceptInput(11L, "자원 소유", true)), List.of()
        );

        assertThatThrownBy(() -> adapter.evaluate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    @Test
    @DisplayName("길이 제한으로 중단된 로컬 모델 응답은 완료된 평가로 인정하지 않는다")
    void rejectsLengthLimitedResponse() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            byte[] body = successResponse().replace("\"done\": true,", "\"done\": true, \"done_reason\": \"length\",")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/chat");
        OllamaEvaluationAdapter adapter = new OllamaEvaluationAdapter(
                new ObjectMapper(), endpoint, "gpt-oss:20b", "os-evaluator-v1", "low", Duration.ofSeconds(30)
        );
        EvaluationRequest request = new EvaluationRequest(
                1L, "프로세스를 설명하세요.", "자원을 소유한다.", "자원을 소유합니다.",
                List.of(new EvaluationConceptInput(11L, "자원 소유", true)), List.of()
        );

        assertThatThrownBy(() -> adapter.evaluate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider schema");
    }

    private String successResponse() {
        return """
                {
                  "model": "gpt-oss:20b",
                  "message": {"role": "assistant", "content": "{\\"verdict\\":\\"CORRECT\\",\\"feedback\\":\\"정확함\\",\\"strengths\\":[\\"자원 소유 설명\\"],\\"omissions\\":[],\\"misconceptions\\":[],\\"concepts\\":[{\\"conceptId\\":11,\\"verdict\\":\\"CORRECT\\",\\"feedback\\":\\"정확함\\"}],\\"evidenceChunkIds\\":[21]}"},
                  "done": true,
                  "prompt_eval_count": 800,
                  "eval_count": 200
                }
                """;
    }
}
