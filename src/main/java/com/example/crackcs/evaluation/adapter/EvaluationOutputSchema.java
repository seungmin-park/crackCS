package com.example.crackcs.evaluation.adapter;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class EvaluationOutputSchema {

    private final ObjectMapper objectMapper;

    public EvaluationOutputSchema(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode create() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.putArray("required").add("verdict").add("feedback").add("strengths")
                .add("omissions").add("misconceptions").add("concepts").add("evidenceChunkIds");
        ObjectNode properties = schema.putObject("properties");
        properties.set("verdict", verdictSchema());
        properties.set("feedback", stringSchema());
        properties.set("strengths", stringArraySchema());
        properties.set("omissions", stringArraySchema());
        properties.set("misconceptions", stringArraySchema());
        properties.set("evidenceChunkIds", integerArraySchema());
        properties.set("concepts", conceptsSchema());
        return schema;
    }

    private ObjectNode conceptsSchema() {
        ObjectNode concept = objectMapper.createObjectNode();
        concept.put("type", "object");
        concept.put("additionalProperties", false);
        concept.putArray("required").add("conceptId").add("verdict").add("feedback");
        ObjectNode properties = concept.putObject("properties");
        properties.set("conceptId", integerSchema());
        properties.set("verdict", verdictSchema());
        properties.set("feedback", stringSchema());
        ObjectNode concepts = objectMapper.createObjectNode();
        concepts.put("type", "array");
        concepts.set("items", concept);
        return concepts;
    }

    private ObjectNode verdictSchema() {
        ObjectNode verdict = objectMapper.createObjectNode();
        verdict.put("type", "string");
        verdict.putArray("enum").add("CORRECT").add("PARTIALLY_CORRECT").add("INCORRECT").add("NEEDS_REVIEW");
        return verdict;
    }

    private ObjectNode stringSchema() {
        ObjectNode string = objectMapper.createObjectNode();
        string.put("type", "string");
        return string;
    }

    private ObjectNode integerSchema() {
        ObjectNode integer = objectMapper.createObjectNode();
        integer.put("type", "integer");
        return integer;
    }

    private ObjectNode stringArraySchema() {
        ObjectNode strings = objectMapper.createObjectNode();
        strings.put("type", "array");
        strings.set("items", stringSchema());
        return strings;
    }

    private ObjectNode integerArraySchema() {
        ObjectNode integers = objectMapper.createObjectNode();
        integers.put("type", "array");
        integers.set("items", integerSchema());
        return integers;
    }
}
