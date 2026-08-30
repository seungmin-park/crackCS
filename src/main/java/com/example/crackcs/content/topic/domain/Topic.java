package com.example.crackcs.content.topic.domain;

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
        name = "topic",
        uniqueConstraints = @UniqueConstraint(name = "uk_topic_code", columnNames = "code")
)
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Topic parent;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder
    private Topic(Topic parent, String code, String name) {
        this.parent = requireDifferentParent(parent);
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
    }

    public void updateParent(Topic parent) {
        this.parent = requireDifferentParent(parent);
    }

    private Topic requireDifferentParent(Topic parent) {
        if (parent == this) {
            throw new IllegalArgumentException("parent must not be self");
        }
        return parent;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
