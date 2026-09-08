package com.example.crackcs.evaluation.domain;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.question.domain.QuestionConcept;
import com.example.crackcs.evaluation.port.ConceptResult;
import com.example.crackcs.evaluation.port.EvaluationResult;
import com.example.crackcs.learning.domain.Answer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import org.hibernate.annotations.BatchSize;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false, unique = true)
    private Answer answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Verdict verdict;

    @Column
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationConcept> concepts = new ArrayList<>();

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EvaluationEvidence> evidence = new LinkedHashSet<>();

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "evaluation_strength", joinColumns = @JoinColumn(name = "evaluation_id"))
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private List<String> strengths = new ArrayList<>();

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "evaluation_omission", joinColumns = @JoinColumn(name = "evaluation_id"))
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private List<String> omissions = new ArrayList<>();

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "evaluation_misconception", joinColumns = @JoinColumn(name = "evaluation_id"))
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private List<String> misconceptions = new ArrayList<>();

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "evaluator_version", length = 100)
    private String evaluatorVersion;

    @Column(name = "processing_duration_millis")
    private Long processingDurationMillis;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "lease_owner", length = 100)
    private String leaseOwner;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Builder
    private Evaluation(Answer answer) {
        if (answer == null) {
            throw new IllegalArgumentException("answer must not be null");
        }
        this.answer = answer;
        this.status = EvaluationStatus.EVALUATING;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.nextAttemptAt = now;
    }

    void complete(EvaluationResult result) {
        ensureActive();
        ValidatedResult validated = validate(result);
        apply(validated, List.of());
    }

    public void completeWithEvidence(EvaluationResult result, List<KnowledgeChunk> providedChunks) {
        ensureActive();
        ValidatedResult validated = validate(result);
        List<EvaluationEvidence> validatedEvidence = validateEvidence(result, providedChunks);
        apply(validated, validatedEvidence);
    }

    private void apply(ValidatedResult validated, List<EvaluationEvidence> validatedEvidence) {
        LocalDateTime now = LocalDateTime.now();
        this.verdict = validated.verdict();
        this.score = validated.verdict().getScore();
        this.feedback = validated.feedback();
        this.concepts.addAll(validated.concepts().stream().map(it -> EvaluationConcept.from(this, it)).toList());
        this.evidence.addAll(validatedEvidence);
        this.strengths.addAll(validated.strengths());
        this.omissions.addAll(validated.omissions());
        this.misconceptions.addAll(validated.misconceptions());
        this.modelName = validated.modelName();
        this.evaluatorVersion = validated.evaluatorVersion();
        this.processingDurationMillis = validated.durationMillis();
        this.inputTokens = validated.inputTokens();
        this.outputTokens = validated.outputTokens();
        this.failureReason = null;
        this.status = validated.verdict() == Verdict.NEEDS_REVIEW
                ? EvaluationStatus.NEEDS_REVIEW : EvaluationStatus.EVALUATED;
        this.evaluatedAt = now;
        this.updatedAt = now;
        clearLease();
    }

    public void requireReview(String safeReason) {
        ensureActive();
        validateSafeReason(safeReason);
        LocalDateTime now = LocalDateTime.now();
        this.verdict = Verdict.NEEDS_REVIEW;
        this.failureReason = safeReason;
        this.status = EvaluationStatus.NEEDS_REVIEW;
        this.evaluatedAt = now;
        this.updatedAt = now;
        clearLease();
    }

    public void fail(String safeReason) {
        ensureActive();
        validateSafeReason(safeReason);
        LocalDateTime now = LocalDateTime.now();
        this.failureReason = safeReason;
        this.status = EvaluationStatus.FAILED;
        this.evaluatedAt = now;
        this.updatedAt = now;
        clearLease();
    }

    public boolean claim(String workerId, LocalDateTime now, Duration leaseDuration) {
        requireText(workerId, "workerId");
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
        boolean pending = status == EvaluationStatus.EVALUATING && !nextAttemptAt.isAfter(now);
        boolean expired = status == EvaluationStatus.PROCESSING
                && leaseExpiresAt != null && leaseExpiresAt.isBefore(now);
        if (!pending && !expired) {
            return false;
        }
        this.status = EvaluationStatus.PROCESSING;
        this.leaseOwner = workerId.trim();
        this.leaseExpiresAt = now.plus(leaseDuration);
        this.attemptCount++;
        this.updatedAt = now;
        return true;
    }

    public void scheduleRetry(String safeReason, LocalDateTime now, Duration delay) {
        if (status != EvaluationStatus.PROCESSING) {
            throw new IllegalStateException("only processing evaluation can retry");
        }
        validateSafeReason(safeReason);
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        this.failureReason = safeReason;
        this.status = EvaluationStatus.EVALUATING;
        this.nextAttemptAt = now.plus(delay);
        this.updatedAt = now;
        clearLease();
    }

    public boolean hasActiveLease(String workerId, LocalDateTime now) {
        return status == EvaluationStatus.PROCESSING
                && leaseOwner != null
                && leaseOwner.equals(workerId)
                && leaseExpiresAt != null
                && !leaseExpiresAt.isBefore(now);
    }

    public boolean isKnowledgeStateEligible() {
        if (status != EvaluationStatus.EVALUATED || verdict == Verdict.NEEDS_REVIEW) {
            return false;
        }
        Set<Long> requiredIds = answer.getQuestion().getQuestionConcepts().stream()
                .filter(QuestionConcept::isRequired).map(qc -> qc.getConcept().getId()).collect(Collectors.toSet());
        return concepts.stream()
                .noneMatch(c -> requiredIds.contains(c.getConceptId()) && c.getVerdict() == Verdict.NEEDS_REVIEW);
    }

    public List<EvaluationConcept> getConcepts() {
        return Collections.unmodifiableList(concepts);
    }

    private ValidatedResult validate(EvaluationResult result) {
        if (result == null || result.verdict() == null) {
            throw new IllegalArgumentException("result verdict must not be null");
        }
        requireFeedback(result.feedback(), "evaluation feedback");
        if (result.concepts() == null) {
            throw new IllegalArgumentException("concept results must not be null");
        }
        Set<Long> expected = answer.getQuestion().getQuestionConcepts().stream()
                .map(qc -> qc.getConcept().getId()).collect(Collectors.toSet());
        if (expected.contains(null)) {
            throw new IllegalStateException("question concepts must be persisted before evaluation");
        }
        Map<Long, ConceptResult> actual;
        try {
            actual = result.concepts().stream().peek(this::validateConceptResult)
                    .collect(Collectors.toMap(ConceptResult::conceptId, Function.identity()));
        } catch (IllegalStateException duplicate) {
            throw new IllegalArgumentException("concept results must not contain duplicates", duplicate);
        }
        if (!actual.keySet().equals(expected)) {
            throw new IllegalArgumentException("concept results must exactly match question concepts");
        }
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
        requireList(result.strengths(), "strengths");
        requireList(result.omissions(), "omissions");
        requireList(result.misconceptions(), "misconceptions");
        requireText(result.modelName(), "modelName");
        requireText(result.evaluatorVersion(), "evaluatorVersion");
        if (result.durationMillis() < 0 || result.inputTokens() < 0 || result.outputTokens() < 0) {
            throw new IllegalArgumentException("evaluation usage values must not be negative");
        }
        return new ValidatedResult(
                result.verdict(), result.feedback(), List.copyOf(result.concepts()),
                result.strengths(), result.omissions(), result.misconceptions(),
                result.modelName(), result.evaluatorVersion(), result.durationMillis(),
                result.inputTokens(), result.outputTokens()
        );
    }

    private List<EvaluationEvidence> validateEvidence(EvaluationResult result, List<KnowledgeChunk> providedChunks) {
        if (providedChunks == null || providedChunks.isEmpty()) {
            throw new IllegalArgumentException("provided evidence must not be empty");
        }
        if (result.evidenceChunkIds() == null || result.evidenceChunkIds().isEmpty()) {
            throw new IllegalArgumentException("evidence chunk ids must not be empty");
        }
        Map<Long, KnowledgeChunk> providedById = providedChunks.stream().collect(Collectors.toMap(
                KnowledgeChunk::getId, Function.identity()
        ));
        if (providedById.containsKey(null)
                || result.evidenceChunkIds().stream().anyMatch(id -> !providedById.containsKey(id))) {
            throw new IllegalArgumentException("evidence must reference only provided chunks");
        }
        if (new HashSet<>(result.evidenceChunkIds()).size() != result.evidenceChunkIds().size()) {
            throw new IllegalArgumentException("evidence chunk ids must not contain duplicates");
        }
        return result.evidenceChunkIds().stream()
                .map(providedById::get)
                .map(chunk -> EvaluationEvidence.from(this, chunk))
                .toList();
    }

    private void validateConceptResult(ConceptResult result) {
        if (result == null || result.conceptId() == null || result.verdict() == null) {
            throw new IllegalArgumentException("concept result fields must not be null");
        }
        requireFeedback(result.feedback(), "concept feedback");
    }

    private static void requireFeedback(String feedback, String fieldName) {
        if (feedback == null || feedback.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void requireList(List<String> values, String fieldName) {
        if (values == null || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(fieldName + " must contain only non-blank values");
        }
    }

    private static void validateSafeReason(String safeReason) {
        if (safeReason == null || safeReason.isBlank()) {
            throw new IllegalArgumentException("failureReason must not be blank");
        }
        if (safeReason.length() > 1000) {
            throw new IllegalArgumentException("failureReason must be 1000 characters or fewer");
        }
    }

    private void ensureActive() {
        if (status != EvaluationStatus.EVALUATING && status != EvaluationStatus.PROCESSING) {
            throw new IllegalStateException("terminal evaluation cannot transition");
        }
    }

    private void clearLease() {
        this.leaseOwner = null;
        this.leaseExpiresAt = null;
    }

    public Set<EvaluationEvidence> getEvidence() {
        return Collections.unmodifiableSet(evidence);
    }

    public List<String> getStrengths() {
        return Collections.unmodifiableList(strengths);
    }

    public List<String> getOmissions() {
        return Collections.unmodifiableList(omissions);
    }

    public List<String> getMisconceptions() {
        return Collections.unmodifiableList(misconceptions);
    }

    private record ValidatedResult(
            Verdict verdict,
            String feedback,
            List<ConceptResult> concepts,
            List<String> strengths,
            List<String> omissions,
            List<String> misconceptions,
            String modelName,
            String evaluatorVersion,
            long durationMillis,
            long inputTokens,
            long outputTokens
    ) {
    }
}
