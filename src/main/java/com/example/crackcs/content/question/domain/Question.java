package com.example.crackcs.content.question.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "question",
        indexes = @Index(
                name = "idx_question_topic_status_difficulty",
                columnList = "topic_id, status, difficulty"
        )
)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id")
    private Member createdByMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_member_id")
    private Member reviewedByMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionDifficulty difficulty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "reference_answer", nullable = false, columnDefinition = "TEXT")
    private String referenceAnswer;

    @Column(name = "version_series_id", nullable = false, length = 36)
    private String versionSeriesId;

    @Column(name = "question_version", nullable = false)
    private int questionVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionConcept> questionConcepts = new LinkedHashSet<>();

    @Builder
    private Question(Topic topic, Member createdByMember, QuestionDifficulty difficulty, String content, String referenceAnswer) {
        this(topic, createdByMember, difficulty, content, referenceAnswer, UUID.randomUUID().toString(), 1);
    }

    private Question(
            Topic topic,
            Member createdByMember,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer,
            String versionSeriesId,
            int questionVersion
    ) {
        this.topic = requireNonNull(topic, "topic");
        this.createdByMember = requireAdmin(createdByMember, "createdByMember");
        this.origin = QuestionOrigin.ADMIN;
        this.type = QuestionType.NORMAL;
        this.difficulty = requireNonNull(difficulty, "difficulty");
        this.content = requireText(content, "content");
        this.referenceAnswer = requireText(referenceAnswer, "referenceAnswer");
        this.versionSeriesId = requireText(versionSeriesId, "versionSeriesId");
        if (questionVersion < 1) {
            throw new IllegalArgumentException("questionVersion must be positive");
        }
        this.questionVersion = questionVersion;
        this.status = QuestionStatus.DRAFT;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            Topic topic,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        ensureDraft("DRAFT 문제만 수정할 수 있습니다.");
        Topic validatedTopic = requireNonNull(topic, "topic");
        QuestionDifficulty validatedDifficulty = requireNonNull(difficulty, "difficulty");
        String validatedContent = requireText(content, "content");
        String validatedReferenceAnswer = requireText(referenceAnswer, "referenceAnswer");
        if (!questionConcepts.isEmpty() && !samePersistentTopic(this.topic, validatedTopic)) {
            throw new InvalidContentStateException("평가 Concept을 먼저 비운 뒤 Topic을 변경해야 합니다.");
        }

        this.topic = validatedTopic;
        this.difficulty = validatedDifficulty;
        this.content = validatedContent;
        this.referenceAnswer = validatedReferenceAnswer;
        clearReview();
        this.updatedAt = LocalDateTime.now();
    }

    public QuestionConcept addConcept(Concept concept, BigDecimal weight, boolean required) {
        ensureDraft("DRAFT 문제에만 Concept을 연결할 수 있습니다.");
        requireNonNull(concept, "concept");
        if (hasConcept(concept)) {
            throw new IllegalArgumentException("concept must not be duplicated");
        }

        QuestionConcept questionConcept = QuestionConcept.create(this, concept, weight, required);
        questionConcepts.add(questionConcept);
        return questionConcept;
    }

    public void replaceConcepts(List<QuestionConceptAssignment> assignments) {
        ensureDraft("DRAFT 문제의 Concept만 교체할 수 있습니다.");
        requireNonNull(assignments, "assignments");

        Set<QuestionConcept> replacements = new LinkedHashSet<>();
        for (QuestionConceptAssignment assignment : assignments) {
            requireNonNull(assignment, "assignment");
            Concept concept = requireNonNull(assignment.concept(), "concept");
            boolean duplicated = replacements.stream()
                    .map(QuestionConcept::getConcept)
                    .anyMatch(existing -> existing == concept || samePersistentConcept(existing, concept));
            if (duplicated) {
                throw new IllegalArgumentException("concept must not be duplicated");
            }
            replacements.add(QuestionConcept.create(
                    this,
                    concept,
                    assignment.weight(),
                    assignment.required()
            ));
        }

        questionConcepts.clear();
        questionConcepts.addAll(replacements);
        clearReview();
        updatedAt = LocalDateTime.now();
    }

    public void review(Member reviewer) {
        ensureDraft("DRAFT 문제만 검수할 수 있습니다.");
        this.reviewedByMember = requireAdmin(reviewer, "reviewer");
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = reviewedAt;
    }

    public void publish() {
        ensureDraft("DRAFT 문제만 공개할 수 있습니다.");
        if (createdByMember == null) {
            throw new InvalidContentStateException("NORMAL 문제는 등록 관리자가 있어야 공개할 수 있습니다.");
        }
        if (reviewedByMember == null || reviewedAt == null) {
            throw new InvalidContentStateException("검수자와 검수 시각이 있어야 문제를 공개할 수 있습니다.");
        }
        if (questionConcepts.isEmpty()) {
            throw new InvalidContentStateException("published question must have at least one concept");
        }
        if (questionConcepts.stream().noneMatch(QuestionConcept::isRequired)) {
            throw new InvalidContentStateException("published question must have at least one required concept");
        }
        BigDecimal weightSum = questionConcepts.stream()
                .map(QuestionConcept::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weightSum.compareTo(BigDecimal.ONE) != 0) {
            throw new InvalidContentStateException("question concept weights must sum to 1.00");
        }

        this.status = QuestionStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void retire() {
        if (status != QuestionStatus.PUBLISHED) {
            throw new InvalidContentStateException("PUBLISHED 문제만 폐기할 수 있습니다.");
        }
        this.status = QuestionStatus.RETIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getTopicId() {
        return topic.getId();
    }

    public Long getCreatedByMemberId() {
        return createdByMember == null ? null : createdByMember.getId();
    }

    public Long getReviewedByMemberId() {
        return reviewedByMember == null ? null : reviewedByMember.getId();
    }

    public Question createNextVersion(
            int nextVersion,
            Member creator,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        if (status != QuestionStatus.PUBLISHED) {
            throw new InvalidContentStateException("PUBLISHED 문제에서만 새 버전을 만들 수 있습니다.");
        }
        Question next = new Question(
                topic,
                creator,
                difficulty,
                content,
                referenceAnswer,
                versionSeriesId,
                nextVersion
        );
        for (QuestionConcept questionConcept : questionConcepts) {
            next.addConcept(
                    questionConcept.getConcept(),
                    questionConcept.getWeight(),
                    questionConcept.isRequired()
            );
        }
        return next;
    }

    public Set<QuestionConcept> getQuestionConcepts() {
        return Collections.unmodifiableSet(questionConcepts);
    }

    private boolean hasConcept(Concept concept) {
        return questionConcepts.stream()
                .map(QuestionConcept::getConcept)
                .anyMatch(existing -> existing == concept || samePersistentConcept(existing, concept));
    }

    private boolean samePersistentConcept(Concept existing, Concept candidate) {
        return existing.getId() != null && existing.getId().equals(candidate.getId());
    }

    private boolean samePersistentTopic(Topic existing, Topic candidate) {
        return existing == candidate
                || (existing.getId() != null && existing.getId().equals(candidate.getId()));
    }

    private void ensureDraft(String message) {
        if (status != QuestionStatus.DRAFT) {
            throw new InvalidContentStateException(message);
        }
    }

    private void clearReview() {
        reviewedByMember = null;
        reviewedAt = null;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static Member requireAdmin(Member member, String fieldName) {
        requireNonNull(member, fieldName);
        if (member.getRole() != MemberRole.ADMIN) {
            throw new IllegalArgumentException(fieldName + " must be an ADMIN member");
        }
        return member;
    }
}
