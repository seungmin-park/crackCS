package com.example.crackcs.evaluation.adapter.ollama;

import com.example.crackcs.evaluation.adapter.EvaluationOutputSchema;
import com.example.crackcs.evaluation.adapter.EvaluationPrompt;
import com.example.crackcs.evaluation.adapter.EvaluationResultParser;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.exception.EvaluationTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "crackcs.evaluation.ollama.enabled", havingValue = "true")
@ConditionalOnProperty(name = "crackcs.evaluation.openai.enabled", havingValue = "false", matchIfMissing = true)
public class OllamaEvaluationAdapter implements EvaluationPort {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI endpoint;
    private final String modelName;
    private final String reasoningEffort;
    private final Duration timeout;
    private final EvaluationPrompt evaluationPrompt;
    private final EvaluationOutputSchema evaluationOutputSchema;
    private final EvaluationResultParser resultParser;

    public OllamaEvaluationAdapter(
            ObjectMapper objectMapper,
            @Value("${crackcs.evaluation.ollama.endpoint:http://127.0.0.1:11434/api/chat}") URI endpoint,
            @Value("${crackcs.evaluation.ollama.model:gpt-oss:20b}") String modelName,
            @Value("${crackcs.evaluation.ollama.evaluator-version:os-evaluator-v1}") String evaluatorVersion,
            @Value("${crackcs.evaluation.ollama.reasoning-effort:medium}") String reasoningEffort,
            @Value("${crackcs.evaluation.ollama.timeout:3m}") Duration timeout
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.endpoint = endpoint;
        this.modelName = modelName;
        this.reasoningEffort = reasoningEffort;
        this.timeout = timeout;
        this.evaluationPrompt = new EvaluationPrompt(objectMapper);
        this.evaluationOutputSchema = new EvaluationOutputSchema(objectMapper);
        this.resultParser = new EvaluationResultParser(objectMapper, modelName,
                evaluatorVersion + "-" + reasoningEffort);
    }

    @Override
    public EvaluationResult evaluate(EvaluationRequest evaluationRequest) {
        long started = System.nanoTime();
        String requestBody = createRequest(evaluationRequest);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama response status " + response.statusCode());
            }
            long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
            return parseResponse(response.body(), durationMillis);
        } catch (HttpTimeoutException failure) {
            throw new EvaluationTimeoutException();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama request interrupted", failure);
        } catch (IOException failure) {
            throw new IllegalStateException("Ollama request failed", failure);
        }
    }

    private String createRequest(EvaluationRequest evaluationRequest) {
        String inputData = evaluationPrompt.data(evaluationRequest);
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", modelName);
        request.put("stream", false);
        request.put("think", reasoningEffort);
        request.set("format", evaluationOutputSchema.create());
        ArrayNode messages = request.putArray("messages");
        messages.add(message("system", evaluationPrompt.instruction()));
        messages.add(message("user", inputData));
        return objectMapper.writeValueAsString(request);
    }

    private ObjectNode message(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private EvaluationResult parseResponse(String ollamaBody, long durationMillis) {
        JsonNode ollama;
        try {
            ollama = objectMapper.readTree(ollamaBody);
        } catch (RuntimeException invalidJson) {
            throw new IllegalArgumentException("provider schema is invalid", invalidJson);
        }
        JsonNode promptTokens = ollama.path("prompt_eval_count");
        JsonNode completionTokens = ollama.path("eval_count");
        JsonNode doneReason = ollama.path("done_reason");
        if (!ollama.path("done").isBoolean() || !ollama.path("done").booleanValue()
                || (!doneReason.isMissingNode() && !"stop".equals(doneReason.stringValue()))
                || !validTokenCount(promptTokens) || !validTokenCount(completionTokens)
                || !modelName.equals(ollama.path("model").stringValue())) {
            throw new IllegalArgumentException("provider schema is invalid");
        }
        JsonNode content = ollama.path("message").path("content");
        if (!content.isString() || content.stringValue().isBlank()) {
            throw new IllegalArgumentException("provider schema is invalid");
        }
        return resultParser.parse(content.stringValue(), durationMillis,
                promptTokens.longValue(), completionTokens.longValue());
    }

    private boolean validTokenCount(JsonNode tokenCount) {
        return tokenCount.isIntegralNumber() && tokenCount.canConvertToLong() && tokenCount.longValue() >= 0;
    }
}
