package com.example.crackcs.content.question.domain;

import com.example.crackcs.content.concept.domain.Concept;

import java.math.BigDecimal;

public record QuestionConceptAssignment(
        Concept concept,
        BigDecimal weight,
        boolean required
) {
}
