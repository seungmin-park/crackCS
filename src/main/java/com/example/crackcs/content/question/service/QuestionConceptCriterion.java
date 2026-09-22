package com.example.crackcs.content.question.service;

import java.math.BigDecimal;

public record QuestionConceptCriterion(
        Long conceptId,
        BigDecimal weight,
        boolean required
) {
}
