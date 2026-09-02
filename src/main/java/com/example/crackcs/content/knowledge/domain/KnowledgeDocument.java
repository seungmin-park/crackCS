package com.example.crackcs.content.knowledge.domain;

import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.content.topic.domain.Topic;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "knowledge_document",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_knowledge_document_checksum", columnNames = "checksum"),
                @UniqueConstraint(
                        name = "uk_knowledge_document_series_version",
                        columnNames = {"version_series_id", "document_version"}
                )
        },
        indexes = @Index(
                name = "idx_knowledge_document_topic_status_technology",
                columnList = "topic_id, status, technology_version"
        )
)
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_member_id", nullable = false)
    private Member createdByMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_member_id")
    private Member reviewedByMember;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private KnowledgeSourceType sourceType;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "version_series_id", nullable = false, length = 36)
    private String versionSeriesId;

    @Column(name = "document_version", nullable = false)
    private int documentVersion;

    @Column(name = "technology_version", length = 100)
    private String technologyVersion;

    @Column(name = "license_note", length = 500)
    private String licenseNote;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeDocumentStatus status;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private KnowledgeDocument(
            Topic topic,
            Member createdByMember,
            String title,
            KnowledgeSourceType sourceType,
            String sourceUrl,
            String technologyVersion,
            String licenseNote,
            String content
    ) {
        this(
                topic,
                createdByMember,
                title,
                sourceType,
                sourceUrl,
                UUID.randomUUID().toString(),
                1,
                technologyVersion,
                licenseNote,
                content
        );
    }

    private KnowledgeDocument(
            Topic topic,
            Member createdByMember,
            String title,
            KnowledgeSourceType sourceType,
            String sourceUrl,
            String versionSeriesId,
            int documentVersion,
            String technologyVersion,
            String licenseNote,
            String content
    ) {
        this.topic = requireNonNull(topic, "topic");
        this.createdByMember = requireAdmin(createdByMember, "createdByMember");
        this.title = requireText(title, "title", 255);
        this.sourceType = requireNonNull(sourceType, "sourceType");
        this.sourceUrl = normalizeSourceUrl(sourceUrl);
        this.versionSeriesId = requireText(versionSeriesId, "versionSeriesId", 36);
        this.documentVersion = requirePositiveVersion(documentVersion);
        this.technologyVersion = normalizeOptionalText(technologyVersion, "technologyVersion", 100);
        this.licenseNote = normalizeOptionalText(licenseNote, "licenseNote", 500);
        this.content = normalizeContent(content);
        this.checksum = ContentChecksum.sha256(this.content);
        this.status = KnowledgeDocumentStatus.DRAFT;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public KnowledgeDocument createNextVersion(
            int nextVersion,
            Topic topic,
            Member createdByMember,
            String title,
            KnowledgeSourceType sourceType,
            String sourceUrl,
            String technologyVersion,
            String licenseNote,
            String content
    ) {
        if (status != KnowledgeDocumentStatus.PUBLISHED) {
            throw new InvalidContentStateException("PUBLISHED 문서에서만 새 버전을 만들 수 있습니다.");
        }
        return new KnowledgeDocument(
                topic,
                createdByMember,
                title,
                sourceType,
                sourceUrl,
                versionSeriesId,
                nextVersion,
                technologyVersion,
                licenseNote,
                content
        );
    }

    public void updateDraft(
            Topic topic,
            String title,
            KnowledgeSourceType sourceType,
            String sourceUrl,
            String technologyVersion,
            String licenseNote,
            String content
    ) {
        ensureDraft("DRAFT 문서만 수정할 수 있습니다.");

        Topic validatedTopic = requireNonNull(topic, "topic");
        String validatedTitle = requireText(title, "title", 255);
        KnowledgeSourceType validatedSourceType = requireNonNull(sourceType, "sourceType");
        String validatedSourceUrl = normalizeSourceUrl(sourceUrl);
        String validatedTechnologyVersion = normalizeOptionalText(
                technologyVersion, "technologyVersion", 100
        );
        String validatedLicenseNote = normalizeOptionalText(licenseNote, "licenseNote", 500);
        String validatedContent = normalizeContent(content);
        String validatedChecksum = ContentChecksum.sha256(validatedContent);

        this.topic = validatedTopic;
        this.title = validatedTitle;
        this.sourceType = validatedSourceType;
        this.sourceUrl = validatedSourceUrl;
        this.technologyVersion = validatedTechnologyVersion;
        this.licenseNote = validatedLicenseNote;
        this.content = validatedContent;
        this.checksum = validatedChecksum;
        clearReview();
        this.updatedAt = LocalDateTime.now();
    }

    public void review(Member reviewer) {
        ensureDraft("DRAFT 문서만 검수할 수 있습니다.");
        this.reviewedByMember = requireAdmin(reviewer, "reviewer");
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = reviewedAt;
    }

    public void publish() {
        ensureDraft("DRAFT 문서만 공개할 수 있습니다.");
        if (reviewedByMember == null || reviewedAt == null) {
            throw new InvalidContentStateException("검수자와 검수 시각이 있어야 문서를 공개할 수 있습니다.");
        }
        validatePublicationSource();
        this.status = KnowledgeDocumentStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void retire() {
        if (status != KnowledgeDocumentStatus.PUBLISHED) {
            throw new InvalidContentStateException("PUBLISHED 문서만 폐기할 수 있습니다.");
        }
        this.status = KnowledgeDocumentStatus.RETIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getTopicId() {
        return topic.getId();
    }

    public Long getCreatedByMemberId() {
        return createdByMember.getId();
    }

    public Long getReviewedByMemberId() {
        return reviewedByMember == null ? null : reviewedByMember.getId();
    }

    private void validatePublicationSource() {
        if (sourceType.isSourceUrlRequired() && sourceUrl == null) {
            throw new InvalidContentStateException("공식 출처 문서는 sourceUrl이 있어야 공개할 수 있습니다.");
        }
        if (technologyVersion == null) {
            throw new InvalidContentStateException("technologyVersion이 있어야 문서를 공개할 수 있습니다.");
        }
        if (licenseNote == null) {
            throw new InvalidContentStateException("licenseNote가 있어야 문서를 공개할 수 있습니다.");
        }
    }

    private void ensureDraft(String message) {
        if (status != KnowledgeDocumentStatus.DRAFT) {
            throw new InvalidContentStateException(message);
        }
    }

    private void clearReview() {
        reviewedByMember = null;
        reviewedAt = null;
    }

    private static String normalizeSourceUrl(String sourceUrl) {
        String normalized = normalizeOptionalText(sourceUrl, "sourceUrl", 1000);
        if (normalized == null) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("sourceUrl must be a valid HTTP URL");
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("sourceUrl must be a valid HTTP URL");
        }
        return normalized;
    }

    private static String normalizeContent(String content) {
        String normalized = requireText(content, "content", Integer.MAX_VALUE)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        return normalized;
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

    private static int requirePositiveVersion(int version) {
        if (version < 1) {
            throw new IllegalArgumentException("documentVersion must be positive");
        }
        return version;
    }

    private static Member requireAdmin(Member member, String fieldName) {
        requireNonNull(member, fieldName);
        if (member.getRole() != MemberRole.ADMIN) {
            throw new IllegalArgumentException(fieldName + " must be an ADMIN member");
        }
        return member;
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
