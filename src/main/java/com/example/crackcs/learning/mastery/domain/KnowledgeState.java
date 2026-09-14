package com.example.crackcs.learning.mastery.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_state", uniqueConstraints = @UniqueConstraint(
        name = "uk_knowledge_state_member_concept", columnNames = {"member_id", "concept_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KnowledgeState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    @Column(nullable = false)
    private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeStatus status;
    private Double masteryScore;
    @Column(nullable = false)
    private int confidenceScore;
    @Column(nullable = false)
    private long attemptCount;
    @Column(nullable = false)
    private long scoreSum;
    @Column(nullable = false)
    private int latestScore;
    private Long latestEvaluationConceptId;
    private LocalDateTime lastEvaluatedAt;
    @Column(nullable = false, length = 30)
    private String algorithmVersion;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private KnowledgeState(Member member, Concept concept) {
        if (member == null || concept == null) {
            throw new IllegalArgumentException("member and concept are required");
        }
        this.member = member;
        this.concept = concept;
        status = KnowledgeStatus.UNKNOWN;
        algorithmVersion = "knowledge-v1";
        createdAt = updatedAt = LocalDateTime.now();
    }

    public void observe(Long evaluationConceptId, Verdict verdict, LocalDateTime evaluatedAt) {
        if (evaluationConceptId == null || evaluationConceptId <= 0 || evaluatedAt == null
                || verdict == null || verdict == Verdict.NEEDS_REVIEW) {
            throw new IllegalArgumentException("a scored concept evaluation and evaluation time are required");
        }
        int score = verdict.getScore();
        long nextSum = Math.addExact(scoreSum, score);
        long nextCount = Math.addExact(attemptCount, 1L);
        long denominator = Math.addExact(nextCount, 1L);
        boolean latest = lastEvaluatedAt == null || evaluatedAt.isAfter(lastEvaluatedAt)
                || (evaluatedAt.equals(lastEvaluatedAt) && evaluationConceptId > latestEvaluationConceptId);
        double nextMastery = (double) Math.addExact(nextSum, latest ? score : latestScore) / denominator;
        LocalDateTime now = LocalDateTime.now();
        scoreSum = nextSum;
        attemptCount = nextCount;
        if (latest) {
            latestScore = score;
            latestEvaluationConceptId = evaluationConceptId;
            lastEvaluatedAt = evaluatedAt;
        }
        masteryScore = nextMastery;
        confidenceScore = attemptCount >= 4 ? 100 : (int) attemptCount * 25;
        status = masteryScore >= 80 && confidenceScore >= 75 ? KnowledgeStatus.STABLE : KnowledgeStatus.LEARNING;
        updatedAt = now;
    }
}
