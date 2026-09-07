package com.example.crackcs.evaluation.domain;

import com.example.crackcs.content.question.domain.QuestionConcept;
import com.example.crackcs.evaluation.port.ConceptResult;
import com.example.crackcs.evaluation.port.EvaluationResult;
import com.example.crackcs.learning.domain.Answer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "evaluation", indexes = @Index(
        name = "idx_evaluation_status_created", columnList = "status, created_at"
))
public class Evaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false, unique = true) private Answer answer;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EvaluationStatus status;
    @Enumerated(EnumType.STRING) @Column(length = 30) private Verdict verdict;
    @Column private Integer score;
    @Column(columnDefinition = "TEXT") private String feedback;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
    @Column(name = "evaluated_at") private LocalDateTime evaluatedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationConcept> concepts = new ArrayList<>();

    @Builder
    private Evaluation(Answer answer) {
        if (answer == null) throw new IllegalArgumentException("answer must not be null");
        this.answer = answer;
        this.status = EvaluationStatus.EVALUATING;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void complete(EvaluationResult result) {
        ensureEvaluating();
        ValidatedResult validated = validate(result);
        LocalDateTime now = LocalDateTime.now();
        this.verdict = validated.verdict();
        this.score = validated.verdict().getScore();
        this.feedback = validated.feedback();
        this.concepts.addAll(validated.concepts().stream().map(it -> EvaluationConcept.from(this, it)).toList());
        this.failureReason = null;
        this.status = EvaluationStatus.EVALUATED;
        this.evaluatedAt = now;
        this.updatedAt = now;
    }

    public void fail(String safeReason) {
        ensureEvaluating();
        if (safeReason == null || safeReason.isBlank()) throw new IllegalArgumentException("failureReason must not be blank");
        if (safeReason.length() > 1000) throw new IllegalArgumentException("failureReason must be 1000 characters or fewer");
        LocalDateTime now = LocalDateTime.now();
        this.failureReason = safeReason;
        this.status = EvaluationStatus.FAILED;
        this.evaluatedAt = now;
        this.updatedAt = now;
    }

    public boolean isKnowledgeStateEligible() {
        if (status != EvaluationStatus.EVALUATED || verdict == Verdict.NEEDS_REVIEW) return false;
        Set<Long> requiredIds = answer.getQuestion().getQuestionConcepts().stream()
                .filter(QuestionConcept::isRequired).map(qc -> qc.getConcept().getId()).collect(Collectors.toSet());
        return concepts.stream().noneMatch(c -> requiredIds.contains(c.getConceptId()) && c.getVerdict() == Verdict.NEEDS_REVIEW);
    }

    public List<EvaluationConcept> getConcepts() { return Collections.unmodifiableList(concepts); }

    private ValidatedResult validate(EvaluationResult result) {
        if (result == null || result.verdict() == null) throw new IllegalArgumentException("result verdict must not be null");
        requireFeedback(result.feedback(), "evaluation feedback");
        if (result.concepts() == null) throw new IllegalArgumentException("concept results must not be null");
        Set<Long> expected = answer.getQuestion().getQuestionConcepts().stream()
                .map(qc -> qc.getConcept().getId()).collect(Collectors.toSet());
        if (expected.contains(null)) throw new IllegalStateException("question concepts must be persisted before evaluation");
        Map<Long, ConceptResult> actual;
        try {
            actual = result.concepts().stream().peek(this::validateConceptResult)
                    .collect(Collectors.toMap(ConceptResult::conceptId, Function.identity()));
        } catch (IllegalStateException duplicate) {
            throw new IllegalArgumentException("concept results must not contain duplicates", duplicate);
        }
        if (!actual.keySet().equals(expected)) throw new IllegalArgumentException("concept results must exactly match question concepts");
        Set<Long> required = answer.getQuestion().getQuestionConcepts().stream()
                .filter(QuestionConcept::isRequired)
                .map(questionConcept -> questionConcept.getConcept().getId())
                .collect(Collectors.toSet());
        boolean hasRequiredNeedsReview = required.stream()
                .map(actual::get)
                .anyMatch(concept -> concept.verdict() == Verdict.NEEDS_REVIEW);
        if (hasRequiredNeedsReview && result.verdict() != Verdict.NEEDS_REVIEW) {
            throw new IllegalArgumentException("required concept NEEDS_REVIEW requires overall NEEDS_REVIEW");
        }
        if (result.verdict() == Verdict.CORRECT) {
            boolean hasNonCorrectRequiredConcept = required.stream()
                    .map(actual::get)
                    .anyMatch(concept -> concept.verdict() != Verdict.CORRECT);
            if (hasNonCorrectRequiredConcept) {
                throw new IllegalArgumentException("CORRECT evaluation requires every required concept to be CORRECT");
            }
        }
        return new ValidatedResult(result.verdict(), result.feedback(), List.copyOf(result.concepts()));
    }

    private void validateConceptResult(ConceptResult result) {
        if (result == null || result.conceptId() == null || result.verdict() == null)
            throw new IllegalArgumentException("concept result fields must not be null");
        requireFeedback(result.feedback(), "concept feedback");
    }

    private static void requireFeedback(String feedback, String fieldName) {
        if (feedback == null || feedback.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private void ensureEvaluating() {
        if (status != EvaluationStatus.EVALUATING) throw new IllegalStateException("terminal evaluation cannot transition");
    }

    private record ValidatedResult(Verdict verdict, String feedback, List<ConceptResult> concepts) { }
}
