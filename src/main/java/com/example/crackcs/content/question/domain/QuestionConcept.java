package com.example.crackcs.content.question.domain;

import com.example.crackcs.content.concept.domain.Concept;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "question_concept",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_question_concept_question_concept",
                columnNames = {"question_id", "concept_id"}
        )
)
public class QuestionConcept {

    private static final BigDecimal MIN_WEIGHT_EXCLUSIVE = BigDecimal.ZERO;
    private static final BigDecimal MAX_WEIGHT_INCLUSIVE = BigDecimal.ONE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    static QuestionConcept create(Question question, Concept concept, BigDecimal weight, boolean required) {
        return new QuestionConcept(question, concept, weight, required);
    }

    private QuestionConcept(Question question, Concept concept, BigDecimal weight, boolean required) {
        this.question = requireNonNull(question, "question");
        this.concept = requireNonNull(concept, "concept");
        this.weight = requireValidWeight(weight);
        this.required = required;
    }

    private static BigDecimal requireValidWeight(BigDecimal weight) {
        if (weight == null
                || weight.compareTo(MIN_WEIGHT_EXCLUSIVE) <= 0
                || weight.compareTo(MAX_WEIGHT_INCLUSIVE) > 0
                || weight.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("weight must be greater than 0 and less than or equal to 1");
        }
        return weight.setScale(2);
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
