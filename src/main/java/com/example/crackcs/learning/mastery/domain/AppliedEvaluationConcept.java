package com.example.crackcs.learning.mastery.domain;

import com.example.crackcs.evaluation.domain.EvaluationConcept;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_application", uniqueConstraints = @UniqueConstraint(
        name = "uk_knowledge_application_evaluation_concept", columnNames = "evaluation_concept_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppliedEvaluationConcept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // The named table UNIQUE enforces one application per evaluation concept without an implicit unnamed UNIQUE.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_concept_id", nullable = false)
    private EvaluationConcept evaluationConcept;
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Builder
    private AppliedEvaluationConcept(EvaluationConcept evaluationConcept) {
        if (evaluationConcept == null) {
            throw new IllegalArgumentException("evaluationConcept is required");
        }
        this.evaluationConcept = evaluationConcept;
        appliedAt = LocalDateTime.now();
    }
}
