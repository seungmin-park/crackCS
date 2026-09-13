package com.example.crackcs.learning.mastery.repository;

import com.example.crackcs.learning.mastery.domain.AppliedEvaluationConcept;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedEvaluationConceptRepository extends JpaRepository<AppliedEvaluationConcept, Long> {
    boolean existsByEvaluationConceptId(Long evaluationConceptId);
}
