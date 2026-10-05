package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.exception.EvaluationTimeoutException;
import com.example.crackcs.exception.ProviderRequestRejectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnExpression("${crackcs.evaluation.openai.enabled:false} or ${crackcs.followup.openai.enabled:false}")
public class JdkOpenAiResponsesClient implements OpenAiResponsesClient {

    private static final Set<String> SPEND_LIMIT_CODES = Set.of(
            "credit_balance_exhausted", "project_spend_limit_exceeded",
            "organization_spend_limit_exceeded", "organization_usage_limit_exceeded"
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final URI endpoint;
    private final int maxResponseBytes;

    public JdkOpenAiResponsesClient(String apiKey, URI endpoint) {
        this(apiKey, endpoint, 1_048_576);
    }

    @Autowired
    public JdkOpenAiResponsesClient(
            @Value("${crackcs.evaluation.openai.api-key}") String apiKey,
            @Value("${crackcs.evaluation.openai.endpoint:https://api.openai.com/v1/responses}") URI endpoint,
            @Value("${crackcs.evaluation.openai.max-response-bytes:1048576}") int maxResponseBytes
    ) {
        if (maxResponseBytes < 1) throw new IllegalArgumentException("response byte limit must be positive");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public String createResponse(String requestBody, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        CompletableFuture<HttpResponse<String>> pendingResponse = httpClient.sendAsync(request,
                info -> new BoundedResponseBodySubscriber(maxResponseBytes,
                        info.headers().firstValueAsLong("Content-Length").orElse(-1)));
        try {
            // Bound the entire exchange, including a provider that stalls after sending headers.
            HttpResponse<String> response = pendingResponse.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                rejectPermanentFailure(response);
                throw new OpenAiProviderException("OpenAI response status " + response.statusCode());
            }
            return response.body();
        } catch (TimeoutException timeoutException) {
            pendingResponse.cancel(true);
            throw new EvaluationTimeoutException();
        } catch (InterruptedException interrupted) {
            pendingResponse.cancel(true);
            Thread.currentThread().interrupt();
            throw new OpenAiProviderException("OpenAI request interrupted", interrupted);
        } catch (ExecutionException failure) {
            for (Throwable cause = failure.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof ProviderRequestRejectedException rejected) throw rejected;
                if (cause instanceof HttpTimeoutException) throw new EvaluationTimeoutException();
            }
            throw new OpenAiProviderException("OpenAI request failed", failure);
        }
    }

    private void rejectPermanentFailure(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new ProviderRequestRejectedException("PROVIDER_AUTH_FAILED");
        }
        if (status == 429 && SPEND_LIMIT_CODES.contains(errorCode(response.body()))) {
            throw new ProviderRequestRejectedException("PROVIDER_BUDGET_EXCEEDED");
        }
        if (status >= 400 && status < 500 && status != 408 && status != 429) {
            throw new ProviderRequestRejectedException("PROVIDER_REQUEST_REJECTED");
        }
    }

    private String errorCode(String responseBody) {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            return response.path("error").path("code").stringValue();
        } catch (RuntimeException invalidBody) {
            return "";
        }
    }
}
