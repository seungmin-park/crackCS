package com.example.crackcs.content.concept.domain;

import com.example.crackcs.content.topic.domain.Topic;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "concept",
        uniqueConstraints = @UniqueConstraint(name = "uk_concept_code", columnNames = "code")
)
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean active;

    @Builder
    private Concept(Topic topic, String code, String name, String description) {
        this.topic = requireNonNull(topic, "topic");
        this.code = requireText(code, "code", 100);
        this.name = requireText(name, "name", 150);
        this.description = normalizeDescription(description);
        this.active = true;
    }

    public void update(Topic topic, String code, String name, String description) {
        Topic validatedTopic = requireNonNull(topic, "topic");
        String validatedCode = requireText(code, "code", 100);
        String validatedName = requireText(name, "name", 150);
        String validatedDescription = normalizeDescription(description);

        this.topic = validatedTopic;
        this.code = validatedCode;
        this.name = validatedName;
        this.description = validatedDescription;
    }

    public void deactivate() {
        this.active = false;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        return normalized;
    }

    private static String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
