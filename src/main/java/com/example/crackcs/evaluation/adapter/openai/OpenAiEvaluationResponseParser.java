package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.adapter.EvaluationResultParser;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "crackcs.evaluation.openai.enabled", havingValue = "true")
public class OpenAiEvaluationResponseParser {

    private final ObjectMapper objectMapper;
    private final EvaluationResultParser resultParser;

    public OpenAiEvaluationResponseParser(
            ObjectMapper objectMapper,
            @Value("${crackcs.evaluation.openai.model:gpt-5.6-terra}") String modelName,
            @Value("${crackcs.evaluation.openai.evaluator-version:os-evaluator-v1}") String evaluatorVersion
    ) {
        this.objectMapper = objectMapper;
        this.resultParser = new EvaluationResultParser(objectMapper, modelName, evaluatorVersion);
    }

    public EvaluationResult parse(String responseBody, long durationMillis) {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            String evaluationJson = findOutputText(response);
            long inputTokens = requiredNonNegativeLong(response.at("/usage/input_tokens"));
            long outputTokens = requiredNonNegativeLong(response.at("/usage/output_tokens"));
            return resultParser.parse(evaluationJson, durationMillis, inputTokens, outputTokens);
        } catch (IllegalArgumentException failure) {
            if (failure.getMessage() != null && failure.getMessage().contains("provider schema")) {
                throw failure;
            }
            throw schemaFailure(failure);
        } catch (RuntimeException failure) {
            throw schemaFailure(failure);
        }
    }

    private String findOutputText(JsonNode response) {
        JsonNode output = response.get("output");
        if (output == null || !output.isArray()) {
            throw schemaFailure();
        }
        for (JsonNode item : output) {
            JsonNode content = item.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode part : content) {
                if ("output_text".equals(part.path("type").stringValue()) && part.has("text")) {
                    return part.get("text").stringValue();
                }
            }
        }
        throw schemaFailure();
    }

    private long requiredNonNegativeLong(JsonNode value) {
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
            throw schemaFailure();
        }
        return value.longValue();
    }

    private IllegalArgumentException schemaFailure() {
        return new IllegalArgumentException("provider schema is invalid");
    }

    private IllegalArgumentException schemaFailure(Throwable cause) {
        return new IllegalArgumentException("provider schema is invalid", cause);
    }
}
