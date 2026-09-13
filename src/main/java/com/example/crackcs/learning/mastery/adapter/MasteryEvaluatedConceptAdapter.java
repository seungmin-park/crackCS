package com.example.crackcs.learning.mastery.adapter;

import com.example.crackcs.evaluation.port.EvaluatedConceptApplicationPort;
import com.example.crackcs.learning.mastery.service.KnowledgeStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MasteryEvaluatedConceptAdapter implements EvaluatedConceptApplicationPort {
    private final KnowledgeStateService knowledgeStates;

    @Override
    public void applyInCurrentTransaction(Long evaluationId) {
        knowledgeStates.applyInCurrentTransaction(evaluationId);
    }
}
