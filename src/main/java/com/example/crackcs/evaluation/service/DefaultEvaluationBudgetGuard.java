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
    private final EvaluationCostPolicy policy;

    public DefaultEvaluationBudgetGuard(
            EvaluationRepository evaluationRepository,
            @Value("${crackcs.evaluation.monthly-budget-usd:30}") BigDecimal monthlyCapUsd,
            @Value("${crackcs.evaluation.input-usd-per-million-tokens:2}") BigDecimal inputPrice,
            @Value("${crackcs.evaluation.output-usd-per-million-tokens:12}") BigDecimal outputPrice
    ) {
        this.evaluationRepository = evaluationRepository;
        this.policy = new EvaluationCostPolicy(monthlyCapUsd, inputPrice, outputPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEvaluate() {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime until = firstDay.plusMonths(1).atStartOfDay();
        return policy.canEvaluate(
                evaluationRepository.sumInputTokensBetween(from, until),
                evaluationRepository.sumOutputTokensBetween(from, until)
        );
    }
}
