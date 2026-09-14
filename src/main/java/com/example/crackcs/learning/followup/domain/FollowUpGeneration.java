package com.example.crackcs.learning.followup.domain;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.learning.answer.domain.Answer;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "follow_up_generation", uniqueConstraints =
@UniqueConstraint(name = "uk_follow_up_generation_answer", columnNames = "answer_id"))
public class FollowUpGeneration {
    // 최초 실행과 만료 후 재선점을 모두 포함한 총 시도 횟수.
    private static final int MAX_GENERATION_ATTEMPTS = 3;
    private static final int MAX_LEASE_TOKEN_LENGTH = 36;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", unique = true)
    private Question question;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowUpStatus status;
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private FollowUpReason reason;
    @Column(nullable = false)
    private int attemptCount;
    @Column(length = MAX_LEASE_TOKEN_LENGTH)
    private String leaseToken;
    private LocalDateTime leaseExpiresAt;
    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Column(length = 100)
    private String modelName;
    @Column(length = 100)
    private String generatorVersion;
    private Long durationMillis;
    private Long inputTokens;
    private Long outputTokens;
    @ElementCollection
    @CollectionTable(name = "follow_up_generation_evidence", joinColumns = @JoinColumn(name = "generation_id"))
    @Column(name = "chunk_id", nullable = false)
    private List<Long> evidenceChunkIds = new ArrayList<>();

    @Builder
    private FollowUpGeneration(Answer answer) {
        if (answer == null) {
            throw new IllegalArgumentException("answer is required");
        }
        this.answer = answer;
        this.status = FollowUpStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
        this.nextAttemptAt = createdAt;
    }

    public boolean claim(String token, LocalDateTime now, Duration duration) {
        requireClaimInput(token, now, duration);
        if (!isReadyForAttempt(now)) {
            return false;
        }
        if (hasExhaustedAttempts()) {
            finish(FollowUpStatus.FAILED, FollowUpReason.ATTEMPTS_EXHAUSTED, now);
            return false;
        }
        LocalDateTime expiresAt = now.plus(duration);
        status = FollowUpStatus.PROCESSING;
        leaseToken = token;
        leaseExpiresAt = expiresAt;
        attemptCount++;
        updatedAt = now;
        return true;
    }

    public boolean hasActiveLease(String token, LocalDateTime now) {
        return status == FollowUpStatus.PROCESSING && Objects.equals(leaseToken, token)
                && now != null && leaseExpiresAt.isAfter(now);
    }

    public void retry(String token, LocalDateTime now, FollowUpReason reason, Duration delay) {
        requireLease(token, now);
        requireFailureReason(reason);
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException("retry delay must not be negative");
        }
        LocalDateTime retryAt = now.plus(delay);
        finish(hasExhaustedAttempts() ? FollowUpStatus.FAILED : FollowUpStatus.PENDING, reason, now);
        nextAttemptAt = retryAt;
    }

    public void fail(String token, LocalDateTime now, FollowUpReason reason) {
        requireLease(token, now);
        requireFailureReason(reason);
        finish(FollowUpStatus.FAILED, reason, now);
    }

    public void unavailable(String token, LocalDateTime now, FollowUpReason reason) {
        requireLease(token, now);
        if (!isUnavailabilityReason(reason)) {
            throw new IllegalArgumentException("unavailability reason is required");
        }
        finish(FollowUpStatus.UNAVAILABLE, reason, now);
    }

    public void complete(String token, LocalDateTime now, Question question, FollowUpResult result) {
        requireLease(token, now);
        requireMatchingQuestion(question, result);
        recordGeneratedQuestion(question, result);
        finish(FollowUpStatus.READY, null, now);
    }

    private void recordGeneratedQuestion(Question question, FollowUpResult result) {
        this.question = question;
        this.modelName = result.modelName();
        this.generatorVersion = result.generatorVersion();
        this.durationMillis = result.durationMillis();
        this.inputTokens = result.inputTokens();
        this.outputTokens = result.outputTokens();
        this.evidenceChunkIds.addAll(result.evidenceChunkIds());
    }

    public List<Long> getEvidenceChunkIds() {
        return Collections.unmodifiableList(evidenceChunkIds);
    }

    private void requireClaimInput(String token, LocalDateTime now, Duration duration) {
        if (!isValidLeaseToken(token) || now == null || !isPositiveDuration(duration)) {
            throw new IllegalArgumentException("valid claim token, time and duration are required");
        }
    }

    private boolean isValidLeaseToken(String token) {
        return token != null && !token.isBlank() && token.length() <= MAX_LEASE_TOKEN_LENGTH;
    }

    private boolean isPositiveDuration(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }

    private boolean isReadyForAttempt(LocalDateTime now) {
        return isPendingAndDue(now) || hasExpiredProcessingLease(now);
    }

    private boolean isPendingAndDue(LocalDateTime now) {
        return status == FollowUpStatus.PENDING && !nextAttemptAt.isAfter(now);
    }

    private boolean hasExpiredProcessingLease(LocalDateTime now) {
        return status == FollowUpStatus.PROCESSING && !leaseExpiresAt.isAfter(now);
    }

    private boolean hasExhaustedAttempts() {
        return attemptCount >= MAX_GENERATION_ATTEMPTS;
    }

    private void requireMatchingQuestion(Question question, FollowUpResult result) {
        if (!isQuestionFromThisAnswer(question) || result == null) {
            throw new IllegalArgumentException("matching follow-up question and result are required");
        }
        if (!matchesGeneratedContent(question, result) || !containsGeneratedConcept(question, result)) {
            throw new IllegalArgumentException("matching follow-up question and result are required");
        }
    }

    private boolean isQuestionFromThisAnswer(Question question) {
        return question != null && question.getSourceAnswer() != null
                && sameSourceAnswer(question.getSourceAnswer());
    }

    private boolean matchesGeneratedContent(Question question, FollowUpResult result) {
        return question.getStatus() == QuestionStatus.PUBLISHED
                && question.getContent().equals(result.content())
                && question.getReferenceAnswer().equals(result.referenceAnswer());
    }

    private boolean containsGeneratedConcept(Question question, FollowUpResult result) {
        return question.getQuestionConcepts().stream()
                .anyMatch(concept -> Objects.equals(concept.getConcept().getId(), result.conceptId()));
    }

    private boolean isUnavailabilityReason(FollowUpReason reason) {
        return reason == FollowUpReason.CONTENT_UNAVAILABLE || reason == FollowUpReason.FOLLOW_UP_LIMIT
                || reason == FollowUpReason.EVALUATION_NOT_ELIGIBLE;
    }

    private boolean sameSourceAnswer(Answer source) {
        return source == answer || answer.getId() != null && answer.getId().equals(source.getId());
    }

    private void requireLease(String token, LocalDateTime now) {
        if (!hasActiveLease(token, now)) {
            throw new IllegalStateException("active generation lease is required");
        }
    }

    private void requireFailureReason(FollowUpReason reason) {
        if (reason == null || isUnavailabilityReason(reason)) {
            throw new IllegalArgumentException("failure reason is required");
        }
    }

    private void finish(FollowUpStatus next, FollowUpReason reason, LocalDateTime now) {
        this.status = next;
        this.reason = reason;
        this.leaseToken = null;
        this.leaseExpiresAt = null;
        this.updatedAt = now;
    }
}
