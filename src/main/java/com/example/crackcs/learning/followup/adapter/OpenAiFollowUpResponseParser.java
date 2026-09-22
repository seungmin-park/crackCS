package com.example.crackcs.learning.followup.adapter;

import com.example.crackcs.learning.followup.domain.FollowUpGenerationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "crackcs.followup.openai.enabled", havingValue = "true")
public class OpenAiFollowUpResponseParser {

    private static final String GENERATOR_VERSION = "follow-up-v1";
    private static final Set<String> REQUIRED_RESULT_FIELDS =
            Set.of("content", "referenceAnswer", "conceptId", "evidenceChunkIds");

    private final ObjectMapper objectMapper;
    private final String modelName;

    public OpenAiFollowUpResponseParser(
            ObjectMapper objectMapper,
            @Value("${crackcs.followup.openai.model:${crackcs.evaluation.openai.model:gpt-5.6-terra}}")
            String modelName
    ) {
        this.objectMapper = objectMapper;
        this.modelName = modelName;
    }

    public FollowUpGenerationResult parse(String responseBody, long durationMillis) {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            JsonNode generatedQuestion = objectMapper.readTree(extractSingleOutputText(response));
            requireExactResultFields(generatedQuestion);
            return new FollowUpGenerationResult(
                    requireNonBlankText(generatedQuestion.path("content")),
                    requireNonBlankText(generatedQuestion.path("referenceAnswer")),
                    requireIntegerAtLeast(generatedQuestion.path("conceptId"), 1),
                    parseEvidenceIds(generatedQuestion.path("evidenceChunkIds")),
                    modelName,
                    GENERATOR_VERSION,
                    durationMillis,
                    requireIntegerAtLeast(response.at("/usage/input_tokens"), 0),
                    requireIntegerAtLeast(response.at("/usage/output_tokens"), 0)
            );
        } catch (JacksonException | IllegalArgumentException invalidOutput) {
            throw invalid(invalidOutput);
        }
    }

    private String extractSingleOutputText(JsonNode response) {
        JsonNode output = response.path("output");
        if (!output.isArray()) {
            throw invalid();
        }
        List<String> outputTexts = new ArrayList<>();
        for (JsonNode item : output) {
            collectOutputTexts(item, outputTexts);
        }
        if (outputTexts.size() != 1) {
            throw invalid();
        }
        return outputTexts.getFirst();
    }

    private void collectOutputTexts(JsonNode item, List<String> outputTexts) {
        JsonNode content = item.path("content");
        if (!content.isArray()) {
            return;
        }
        for (JsonNode part : content) {
            if ("output_text".equals(part.path("type").stringValue())) {
                outputTexts.add(requireNonBlankText(part.path("text")));
            }
        }
    }

    private void requireExactResultFields(JsonNode generatedQuestion) {
        if (!generatedQuestion.isObject()) {
            throw invalid();
        }
        Set<String> fields = new HashSet<>();
        generatedQuestion.propertyStream().forEach(entry -> fields.add(entry.getKey()));
        if (!fields.equals(REQUIRED_RESULT_FIELDS)) {
            throw invalid();
        }
    }

    private List<Long> parseEvidenceIds(JsonNode evidenceChunkIds) {
        if (!evidenceChunkIds.isArray()) {
            throw invalid();
        }
        List<Long> ids = new ArrayList<>();
        for (JsonNode evidenceChunkId : evidenceChunkIds) {
            ids.add(requireIntegerAtLeast(evidenceChunkId, 1));
        }
        return ids;
    }

    private String requireNonBlankText(JsonNode value) {
        if (!value.isString() || value.stringValue().isBlank()) {
            throw invalid();
        }
        return value.stringValue();
    }

    private long requireIntegerAtLeast(JsonNode value, long minimum) {
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < minimum) {
            throw invalid();
        }
        return value.longValue();
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("invalid follow-up provider result");
    }

    private IllegalArgumentException invalid(Throwable cause) {
        return new IllegalArgumentException("invalid follow-up provider result", cause);
    }
}
