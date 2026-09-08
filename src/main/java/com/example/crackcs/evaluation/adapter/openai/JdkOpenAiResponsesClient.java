package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.exception.EvaluationTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "crackcs.evaluation.openai.enabled", havingValue = "true")
public class JdkOpenAiResponsesClient implements OpenAiResponsesClient {

    private final HttpClient client;
    private final String apiKey;
    private final URI endpoint;

    public JdkOpenAiResponsesClient(
            @Value("${crackcs.evaluation.openai.api-key}") String apiKey,
            @Value("${crackcs.evaluation.openai.endpoint:https://api.openai.com/v1/responses}") URI endpoint
    ) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public String createResponse(String requestBody, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OpenAiProviderException("OpenAI response status " + response.statusCode());
            }
            return response.body();
        } catch (HttpTimeoutException timeoutException) {
            throw new EvaluationTimeoutException();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new OpenAiProviderException("OpenAI request interrupted", interrupted);
        } catch (IOException failure) {
            throw new OpenAiProviderException("OpenAI request failed", failure);
        }
    }
}
