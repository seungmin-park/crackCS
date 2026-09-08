package com.example.crackcs.evaluation.domain;

import com.example.crackcs.evaluation.port.ConceptResult;
import com.example.crackcs.content.concept.domain.Concept;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "evaluation_concept", uniqueConstraints = @UniqueConstraint(
        name = "uk_evaluation_concept_evaluation_concept", columnNames = {"evaluation_id", "concept_id"}
))
public class EvaluationConcept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Verdict verdict;
    @Column
    private Integer score;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String feedback;

    public Long getConceptId() {
        return concept.getId();
    }

    public String getConceptName() {
        return concept.getName();
    }

    static EvaluationConcept from(Evaluation evaluation, ConceptResult result) {
        EvaluationConcept concept = new EvaluationConcept();
        concept.evaluation = evaluation;
        concept.concept = evaluation.getAnswer().getQuestion().getQuestionConcepts().stream()
                .map(questionConcept -> questionConcept.getConcept())
                .filter(candidate -> candidate.getId().equals(result.conceptId()))
                .findFirst().orElseThrow();
        concept.verdict = result.verdict();
        concept.score = result.verdict().getScore();
        concept.feedback = result.feedback();
        return concept;
    }
}
