package com.example.crackcs.content.question.service;

import java.math.BigDecimal;

public record QuestionConceptData(
        Long conceptId,
        BigDecimal weight,
        boolean required
) {
}
