package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.adapter.EvaluationOutputSchema;
import com.example.crackcs.evaluation.adapter.EvaluationPrompt;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
@ConditionalOnProperty(name = "crackcs.evaluation.openai.enabled", havingValue = "true")
public class OpenAiEvaluationRequestFactory {

    private final ObjectMapper objectMapper;
    private final String modelName;
    private final String reasoningEffort;
    private final int maxOutputTokens;
    private final EvaluationPrompt evaluationPrompt;
    private final EvaluationOutputSchema evaluationOutputSchema;

    public OpenAiEvaluationRequestFactory(
            ObjectMapper objectMapper,
            @Value("${crackcs.evaluation.openai.model:gpt-5.6-terra}") String modelName,
            @Value("${crackcs.evaluation.openai.reasoning-effort:low}") String reasoningEffort,
            @Value("${crackcs.evaluation.openai.max-output-tokens:4096}") int maxOutputTokens
    ) {
        if (maxOutputTokens <= 0) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
        this.objectMapper = objectMapper;
        this.modelName = modelName;
        this.reasoningEffort = reasoningEffort;
        this.maxOutputTokens = maxOutputTokens;
        this.evaluationPrompt = new EvaluationPrompt(objectMapper);
        this.evaluationOutputSchema = new EvaluationOutputSchema(objectMapper);
    }

    public String create(EvaluationRequest evaluationRequest) {
        String inputData = evaluationPrompt.data(evaluationRequest);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", modelName);
        root.put("store", false);
        root.put("max_output_tokens", maxOutputTokens);
        root.putObject("reasoning").put("effort", reasoningEffort);
        ArrayNode input = root.putArray("input");
        input.add(message("developer", evaluationPrompt.instruction()));
        input.add(message("user", inputData));
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        format.put("name", "answer_evaluation");
        format.put("strict", true);
        format.set("schema", evaluationOutputSchema.create());
        root.putObject("text").set("format", format);
        return objectMapper.writeValueAsString(root);
    }

    private ObjectNode message(String role, String text) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        ObjectNode content = objectMapper.createObjectNode();
        content.put("type", "input_text");
        content.put("text", text);
        message.putArray("content").add(content);
        return message;
    }
}
