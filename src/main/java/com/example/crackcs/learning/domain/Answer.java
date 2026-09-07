package com.example.crackcs.learning.domain;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "answer", uniqueConstraints = @UniqueConstraint(
        name = "uk_answer_member_request", columnNames = {"member_id", "request_id"}
), indexes = @Index(name = "idx_answer_member_submitted", columnList = "member_id, submitted_at"))
public class Answer {
    private static final int MAX_CONTENT_LENGTH = 10_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Builder
    private Answer(Member member, Question question, String requestId, String content) {
        this.member = requireNonNull(member, "member");
        if (!member.isAuthenticatable() || member.getRole() != com.example.crackcs.member.domain.MemberRole.USER) {
            throw new IllegalArgumentException("active USER member is required");
        }
        this.question = requirePublished(question);
        this.requestId = requireCanonicalUuid(requestId);
        this.content = requireContent(content);
        this.submittedAt = LocalDateTime.now();
    }

    private static Question requirePublished(Question question) {
        requireNonNull(question, "question");
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalStateException("PUBLISHED question is required");
        }
        return question;
    }

    private static String requireCanonicalUuid(String requestId) {
        if (requestId == null) throw new IllegalArgumentException("requestId must not be null");
        try {
            String canonical = UUID.fromString(requestId).toString();
            if (!canonical.equals(requestId)) throw new IllegalArgumentException("requestId must be a canonical UUID");
            return canonical;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("requestId must be a canonical UUID", exception);
        }
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("content must not be blank");
        if (content.length() > MAX_CONTENT_LENGTH) throw new IllegalArgumentException("content must be 10000 characters or fewer");
        return content;
    }

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
        return value;
    }
}
