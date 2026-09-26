package com.example.crackcs.evaluation.adapter;

import com.example.crackcs.evaluation.domain.ConceptResult;
import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EvaluationResultParser {

    private final ObjectMapper objectMapper;
    private final String modelName;
    private final String evaluatorVersion;

    public EvaluationResultParser(
            ObjectMapper objectMapper,
            String modelName,
            String evaluatorVersion
    ) {
        this.objectMapper = objectMapper;
        this.modelName = modelName;
        this.evaluatorVersion = evaluatorVersion;
    }

    public EvaluationResult parse(String evaluationJson, long durationMillis, long inputTokens, long outputTokens) {
        try {
            JsonNode evaluation = objectMapper.readTree(evaluationJson);
            requireExactFields(evaluation, "verdict", "feedback", "strengths", "omissions",
                    "misconceptions", "concepts", "evidenceChunkIds");
            Verdict verdict = Verdict.valueOf(requiredText(evaluation, "verdict"));
            String feedback = requiredText(evaluation, "feedback");
            List<String> strengths = strings(evaluation, "strengths");
            List<String> omissions = strings(evaluation, "omissions");
            List<String> misconceptions = strings(evaluation, "misconceptions");
            List<Long> evidenceChunkIds = longs(evaluation, "evidenceChunkIds");
            List<ConceptResult> conceptResults = concepts(evaluation.get("concepts"));
            if (inputTokens < 0 || outputTokens < 0) {
                throw schemaFailure();
            }
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
