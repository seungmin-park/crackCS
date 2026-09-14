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
    private final EvaluationRepository evaluations;
    private final EvaluationCostPolicy policy;

    public DefaultEvaluationBudgetGuard(
            EvaluationRepository evaluations,
            @Value("${crackcs.evaluation.monthly-budget-usd:30}") BigDecimal monthlyCapUsd,
            @Value("${crackcs.evaluation.input-usd-per-million-tokens:2}") BigDecimal inputPrice,
            @Value("${crackcs.evaluation.output-usd-per-million-tokens:12}") BigDecimal outputPrice
    ) {
        this.evaluations = evaluations;
        this.policy = new EvaluationCostPolicy(monthlyCapUsd, inputPrice, outputPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEvaluate() {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime until = firstDay.plusMonths(1).atStartOfDay();
        return policy.canEvaluate(
                evaluations.sumInputTokensBetween(from, until),
                evaluations.sumOutputTokensBetween(from, until)
        );
    }
}
