package com.example.crackcs.evaluation.repository;

import com.example.crackcs.evaluation.domain.Evaluation;
import jakarta.persistence.EntityManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class EvaluationDetailsRepositoryImpl implements EvaluationDetailsRepository {
    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public List<Evaluation> findAllDetailsByAnswerIds(List<Long> answerIds) {
        if (answerIds.isEmpty()) {
            return List.of();
        }
        List<Evaluation> withConcepts = entityManager.createQuery("""
                select distinct e from Evaluation e
                left join fetch e.concepts c left join fetch c.concept
                where e.answer.id in :answerIds
                """, Evaluation.class).setParameter("answerIds", answerIds).getResultList();
        List<Evaluation> withEvidence = entityManager.createQuery("""
                select distinct e from Evaluation e
                left join fetch e.evidence evidence
                left join fetch evidence.chunk chunk left join fetch chunk.document
                where e.answer.id in :answerIds
                """, Evaluation.class).setParameter("answerIds", answerIds).getResultList();
        // The same persistence context gives each ID one managed instance hydrated by both queries.
        Map<Long, Evaluation> details = new LinkedHashMap<>();
        withConcepts.forEach(evaluation -> details.put(evaluation.getId(), evaluation));
        withEvidence.forEach(evaluation -> details.put(evaluation.getId(), evaluation));
        return List.copyOf(details.values());
    }
}
