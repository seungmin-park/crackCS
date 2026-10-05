package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.exception.ProviderRequestRejectedException;
import com.example.crackcs.exception.EvaluationTimeoutException;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdkOpenAiResponsesClientTest {

    private HttpServer server;

    @ParameterizedTest
    @ValueSource(ints = {200, 503})
    @DisplayName("Content Length가 없는 성공·오류 응답도 수신 byte 한도를 넘으면 거부한다")
    void rejectsOversizedChunkedBody(int status) throws IOException {
        JdkOpenAiResponsesClient client = chunkedClientResponding(status, "x".repeat(131_073), 131_072);

        assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(5)))
                .isInstanceOf(ProviderRequestRejectedException.class)
                .hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
    }

    @Test
    @DisplayName("UTF8 응답이 정확히 byte 한도와 같으면 전체 문자열을 반환한다")
    void acceptsBodyExactlyAtByteLimit() throws IOException {
        String responseBody = "가".repeat(21) + "a";
        JdkOpenAiResponsesClient client = chunkedClientResponding(200, responseBody, 64);

        String result = client.createResponse("{}", Duration.ofSeconds(5));

        assertThat(result).isEqualTo(responseBody);
        assertThat(result.getBytes(StandardCharsets.UTF_8)).hasSize(64);
    }

    @Test
    @DisplayName("문자 수가 작아도 UTF8 byte 한도를 넘은 응답을 거부한다")
    void countsUtf8BytesInsteadOfCharacters() throws IOException {
        JdkOpenAiResponsesClient client = chunkedClientResponding(200, "가".repeat(22), 64);

        assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(5)))
                .isInstanceOf(ProviderRequestRejectedException.class)
                .hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
    }

    @Test
    @DisplayName("큰 Content Length를 받으면 본문 전송 완료를 기다리지 않고 중단한다")
    void rejectsDeclaredOversizeBeforeBodyArrives() throws IOException {
        CountDownLatch releaseBody = new CountDownLatch(1);
        URI endpoint = startServer(exchange -> {
            exchange.sendResponseHeaders(200, 4096);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write('x');
                output.flush();
                awaitRelease(releaseBody);
            }
        });
        JdkOpenAiResponsesClient client = new JdkOpenAiResponsesClient("test-only", endpoint, 64);

        try {
            assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(1)))
                    .isInstanceOf(ProviderRequestRejectedException.class)
                    .hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
        } finally {
            releaseBody.countDown();
        }
    }

    @Test
    @DisplayName("chunked 응답은 초과 chunk 뒤의 미전송 본문을 기다리지 않고 중단한다")
    void abortsChunkedBodyBeforeTransferCompletes() throws IOException {
        CountDownLatch releaseBody = new CountDownLatch(1);
        URI endpoint = startServer(exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(new byte[65]);
                output.flush();
                awaitRelease(releaseBody);
                output.write(new byte[1024]);
            }
        });
        JdkOpenAiResponsesClient client = new JdkOpenAiResponsesClient("test-only", endpoint, 64);

        try {
            assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(1)))
                    .isInstanceOf(ProviderRequestRejectedException.class)
                    .hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
        } finally {
            releaseBody.countDown();
        }
    }

    @Test
    @DisplayName("header 뒤에 본문 전송이 멈춰도 전체 요청 시간 한도로 취소한다")
    void timesOutStalledBodyAfterHeaders() throws Exception {
        CountDownLatch bodyStarted = new CountDownLatch(1);
        CountDownLatch releaseBody = new CountDownLatch(1);
        URI endpoint = startServer(exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write('{');
                output.flush();
                bodyStarted.countDown();
                awaitRelease(releaseBody);
            }
        });
        JdkOpenAiResponsesClient client = new JdkOpenAiResponsesClient("test-only", endpoint, 64);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            Future<String> response = caller.submit(() -> client.createResponse("{}", Duration.ofSeconds(1)));
            assertThat(bodyStarted.await(2, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> response.get(3, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(EvaluationTimeoutException.class);
        } finally {
            releaseBody.countDown();
            caller.shutdownNow();
            assertThat(caller.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("큰 성공 응답은 문자열로 모두 읽기 전에 영구 거부한다")
    void rejectsOversizedSuccessBody() throws IOException {
        JdkOpenAiResponsesClient client = clientResponding(200, "x".repeat(1_048_577));

        assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(5)))
                .isInstanceOf(ProviderRequestRejectedException.class)
                .hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
    }

    @Test
    @DisplayName("큰 오류 응답도 상태 코드 처리 전에 같은 크기 한도로 거부한다")
    void rejectsOversizedErrorBody() throws IOException {
        JdkOpenAiResponsesClient client = clientResponding(503, "x".repeat(1_048_577));

        assertThatThrownBy(() -> client.createResponse("{}", Duration.ofSeconds(5)))
                .isInstanceOf(ProviderRequestRejectedException.class)
                .hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
    }

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
        URI endpoint = startServer(exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        return new JdkOpenAiResponsesClient("test-only", endpoint);
    }

    private JdkOpenAiResponsesClient chunkedClientResponding(int status, String body, int maxBytes) throws IOException {
        URI endpoint = startServer(exchange -> {
            exchange.sendResponseHeaders(status, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body.getBytes(StandardCharsets.UTF_8));
            }
        });
        return new JdkOpenAiResponsesClient("test-only", endpoint, maxBytes);
    }

    private URI startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/responses", handler);
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses");
    }

    private void awaitRelease(CountDownLatch releaseBody) {
        try {
            releaseBody.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
