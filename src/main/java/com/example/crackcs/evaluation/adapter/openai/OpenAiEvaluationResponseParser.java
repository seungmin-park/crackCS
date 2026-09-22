package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.domain.ConceptResult;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "crackcs.evaluation.openai.enabled", havingValue = "true")
public class OpenAiEvaluationResponseParser {

    private final ObjectMapper objectMapper;
    private final String modelName;
    private final String evaluatorVersion;

    public OpenAiEvaluationResponseParser(
            ObjectMapper objectMapper,
            @Value("${crackcs.evaluation.openai.model:gpt-5.6-terra}") String modelName,
            @Value("${crackcs.evaluation.openai.evaluator-version:os-evaluator-v1}") String evaluatorVersion
    ) {
        this.objectMapper = objectMapper;
        this.modelName = modelName;
        this.evaluatorVersion = evaluatorVersion;
    }

    public EvaluationResult parse(String responseBody, long durationMillis) {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode evaluation = objectMapper.readTree(findOutputText(response));
            requireExactFields(evaluation, "verdict", "feedback", "strengths", "omissions",
                    "misconceptions", "concepts", "evidenceChunkIds");
            Verdict verdict = Verdict.valueOf(requiredText(evaluation, "verdict"));
            String feedback = requiredText(evaluation, "feedback");
            List<String> strengths = strings(evaluation, "strengths");
            List<String> omissions = strings(evaluation, "omissions");
            List<String> misconceptions = strings(evaluation, "misconceptions");
            List<Long> evidenceChunkIds = longs(evaluation, "evidenceChunkIds");
            List<ConceptResult> conceptResults = concepts(evaluation.get("concepts"));
            long inputTokens = requiredNonNegativeLong(response.at("/usage/input_tokens"));
            long outputTokens = requiredNonNegativeLong(response.at("/usage/output_tokens"));
            return new EvaluationResult(
                    verdict, feedback, conceptResults, strengths, omissions, misconceptions, evidenceChunkIds,
                    modelName, evaluatorVersion, durationMillis, inputTokens, outputTokens
            );
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

    private List<ConceptResult> concepts(JsonNode values) {
        if (values == null || !values.isArray()) {
            throw schemaFailure();
        }
        List<ConceptResult> conceptResults = new ArrayList<>();
        for (JsonNode value : values) {
            requireExactFields(value, "conceptId", "verdict", "feedback");
            conceptResults.add(new ConceptResult(
                    requiredLong(value, "conceptId"),
                    Verdict.valueOf(requiredText(value, "verdict")),
                    requiredText(value, "feedback")
            ));
        }
        return conceptResults;
    }

    private List<String> strings(JsonNode parent, String field) {
        JsonNode values = parent.get(field);
        if (values == null || !values.isArray()) {
            throw schemaFailure();
        }
        List<String> texts = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isString()) {
                throw schemaFailure();
            }
            texts.add(value.stringValue());
        }
        return texts;
    }

    private List<Long> longs(JsonNode parent, String field) {
        JsonNode values = parent.get(field);
        if (values == null || !values.isArray()) {
            throw schemaFailure();
        }
        List<Long> ids = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isIntegralNumber() || !value.canConvertToLong()) {
                throw schemaFailure();
            }
            ids.add(value.longValue());
        }
        return ids;
    }

    private String requiredText(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw schemaFailure();
        }
        return value.stringValue();
    }

    private long requiredLong(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw schemaFailure();
        }
        return value.longValue();
    }

    private long requiredNonNegativeLong(JsonNode value) {
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
            throw schemaFailure();
        }
        return value.longValue();
    }

    private void requireExactFields(JsonNode object, String... expectedFields) {
        if (object == null || !object.isObject()) {
            throw schemaFailure();
        }
        Set<String> expected = Set.of(expectedFields);
        Set<String> actual = new HashSet<>();
        object.propertyStream().forEach(entry -> actual.add(entry.getKey()));
        if (!actual.equals(expected)) {
            throw schemaFailure();
        }
    }

    private IllegalArgumentException schemaFailure() {
        return new IllegalArgumentException("provider schema is invalid");
    }

    private IllegalArgumentException schemaFailure(Throwable cause) {
        return new IllegalArgumentException("provider schema is invalid", cause);
    }
}
