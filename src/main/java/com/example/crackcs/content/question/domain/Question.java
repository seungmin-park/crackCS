package com.example.crackcs.content.question.domain;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.topic.domain.Topic;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionConcept> questionConcepts = new LinkedHashSet<>();

    @Builder
    private Question(
            Topic topic,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        this.topic = requireNonNull(topic, "topic");
        this.origin = QuestionOrigin.ADMIN;
        this.type = QuestionType.NORMAL;
        this.difficulty = requireNonNull(difficulty, "difficulty");
        this.content = requireText(content, "content");
        this.referenceAnswer = requireText(referenceAnswer, "referenceAnswer");
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
        Topic validatedTopic = requireNonNull(topic, "topic");
        QuestionDifficulty validatedDifficulty = requireNonNull(difficulty, "difficulty");
        String validatedContent = requireText(content, "content");
        String validatedReferenceAnswer = requireText(referenceAnswer, "referenceAnswer");

        this.topic = validatedTopic;
        this.difficulty = validatedDifficulty;
        this.content = validatedContent;
        this.referenceAnswer = validatedReferenceAnswer;
        this.updatedAt = LocalDateTime.now();
    }

    public QuestionConcept addConcept(Concept concept, BigDecimal weight, boolean required) {
        requireNonNull(concept, "concept");
        if (hasConcept(concept)) {
            throw new IllegalArgumentException("concept must not be duplicated");
        }

        QuestionConcept questionConcept = QuestionConcept.create(this, concept, weight, required);
        questionConcepts.add(questionConcept);
        return questionConcept;
    }

    public void publish() {
        if (questionConcepts.isEmpty()) {
            throw new IllegalStateException("published question must have at least one concept");
        }
        if (questionConcepts.stream().noneMatch(QuestionConcept::isRequired)) {
            throw new IllegalStateException("published question must have at least one required concept");
        }

        this.status = QuestionStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void retire() {
        this.status = QuestionStatus.RETIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getTopicId() {
        return topic.getId();
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
        return value;
    }
}
