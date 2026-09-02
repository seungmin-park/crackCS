package com.example.crackcs.content.concept.controller.response;

import com.example.crackcs.content.concept.domain.Concept;

public record ConceptResponse(
        Long id,
        Long topicId,
        String code,
        String name,
        String description,
        boolean active
) {
    public static ConceptResponse from(Concept concept) {
        return new ConceptResponse(
                concept.getId(),
                concept.getTopic().getId(),
                concept.getCode(),
                concept.getName(),
                concept.getDescription(),
                concept.isActive()
        );
    }
}
