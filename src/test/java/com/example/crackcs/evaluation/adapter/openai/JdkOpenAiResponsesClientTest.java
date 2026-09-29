package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.exception.ProviderRequestRejectedException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdkOpenAiResponsesClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("크레딧 부족 응답은 재시도할 수 없는 비용 오류로 분류한다")
    void rejectsExhaustedCreditWithoutRetry() throws IOException {
        JdkOpenAiResponsesClient client = clientResponding(429,
                "{\"error\":{\"code\":\"credit_balance_exhausted\"}}");

        assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(5)))
                .isInstanceOf(ProviderRequestRejectedException.class)
                .hasMessageContaining("PROVIDER_BUDGET_EXCEEDED");
    }

    @Test
    @DisplayName("일시적 요청 속도 제한은 재시도 대상 오류를 유지한다")
    void retriesTemporaryRateLimit() throws IOException {
        JdkOpenAiResponsesClient client = clientResponding(429,
                "{\"error\":{\"code\":\"rate_limit_exceeded\"}}");

        assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(5)))
                .isInstanceOf(OpenAiProviderException.class)
                .isNotInstanceOf(ProviderRequestRejectedException.class);
    }

    @Test
    @DisplayName("오류 코드가 빠진 속도 제한 응답도 안전하게 재시도 오류로 분류한다")
    void retriesRateLimitWithoutCode() throws IOException {
        JdkOpenAiResponsesClient client = clientResponding(429, "{\"error\":{}}");

        assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(5)))
                .isInstanceOf(OpenAiProviderException.class)
                .isNotInstanceOf(ProviderRequestRejectedException.class);
    }

    @Test
    @DisplayName("잘못된 인증은 반복 호출하지 않는 설정 오류로 분류한다")
    void rejectsInvalidCredentialWithoutRetry() throws IOException {
        JdkOpenAiResponsesClient client = clientResponding(401,
                "{\"error\":{\"code\":\"invalid_api_key\"}}");

        assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(5)))
                .isInstanceOf(ProviderRequestRejectedException.class)
                .hasMessageContaining("PROVIDER_AUTH_FAILED");
    }

    private JdkOpenAiResponsesClient clientResponding(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/responses", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses");
        return new JdkOpenAiResponsesClient("test-only", endpoint);
    }
}
