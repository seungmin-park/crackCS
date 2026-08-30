package com.example.crackcs.content.question.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

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

    @Builder
    private Question(
            Long topicId,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        this.topicId = requirePositive(topicId, "topicId");
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
            Long topicId,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        Long validatedTopicId = requirePositive(topicId, "topicId");
        QuestionDifficulty validatedDifficulty = requireNonNull(difficulty, "difficulty");
        String validatedContent = requireText(content, "content");
        String validatedReferenceAnswer = requireText(referenceAnswer, "referenceAnswer");

        this.topicId = validatedTopicId;
        this.difficulty = validatedDifficulty;
        this.content = validatedContent;
        this.referenceAnswer = validatedReferenceAnswer;
        this.updatedAt = LocalDateTime.now();
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
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
