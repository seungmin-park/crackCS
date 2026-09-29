package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.repository.EvaluationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DefaultEvaluationBudgetGuard implements EvaluationBudgetGuard {
    private final EvaluationRepository evaluationRepository;
    private final EvaluationCostPolicy evaluationCostPolicy;
    private final String modelName;

    public DefaultEvaluationBudgetGuard(
            EvaluationRepository evaluationRepository,
            @Value("${crackcs.evaluation.monthly-budget-usd:30}") BigDecimal monthlyCapUsd,
            @Value("${crackcs.evaluation.input-usd-per-million-tokens:2}") BigDecimal inputPrice,
            @Value("${crackcs.evaluation.output-usd-per-million-tokens:12}") BigDecimal outputPrice,
            @Value("${crackcs.evaluation.openai.model:gpt-5.6-terra}") String modelName
    ) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationCostPolicy = new EvaluationCostPolicy(monthlyCapUsd, inputPrice, outputPrice);
        this.modelName = modelName;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEvaluate() {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime until = firstDay.plusMonths(1).atStartOfDay();
        return evaluationCostPolicy.canEvaluate(
                evaluationRepository.sumInputTokensBetweenForModel(from, until, modelName),
                evaluationRepository.sumOutputTokensBetweenForModel(from, until, modelName)
        );
    }
}
