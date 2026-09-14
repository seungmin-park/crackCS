package com.example.crackcs.evaluation.domain;

import com.example.crackcs.content.concept.domain.Concept;
import jakarta.persistence.*;
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

    public Long getConceptId() {
        return concept.getId();
    }

    public String getConceptName() {
        return concept.getName();
    }
}
