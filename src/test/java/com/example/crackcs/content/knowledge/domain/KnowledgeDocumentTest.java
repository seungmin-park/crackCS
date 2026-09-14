package com.example.crackcs.content.knowledge.domain;

import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeDocumentTest {

    @Test
    @DisplayName("최초 KnowledgeDocument는 버전 1의 DRAFT로 생성되고 원문 checksum을 계산한다")
    void createsFirstDraftWithChecksum() {
        KnowledgeDocument document = createDocument("첫 줄\r\n둘째 줄");

        assertThat(document.getDocumentVersion()).isEqualTo(1);
        assertThat(document.getStatus()).isEqualTo(KnowledgeDocumentStatus.DRAFT);
        assertThat(document.getContent()).isEqualTo("첫 줄\n둘째 줄");
        assertThat(document.getChecksum()).isEqualTo(ContentChecksum.sha256("첫 줄\n둘째 줄"));
        assertThat(document.getVersionSeriesId()).isNotBlank();
        assertThat(document.getCreatedAt()).isEqualTo(document.getUpdatedAt());
    }

    @Test
    @DisplayName("검수 정보와 출처 정보가 있는 DRAFT 문서를 공개한다")
    void reviewsAndPublishesDraft() {
        KnowledgeDocument document = createDocument("원문");

        document.review(admin("검수자"));
        document.publish();

        assertThat(document.getStatus()).isEqualTo(KnowledgeDocumentStatus.PUBLISHED);
        assertThat(document.getReviewedByMember()).isNotNull();
        assertThat(document.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("검수하지 않은 문서는 공개할 수 없다")
    void rejectsPublishWithoutReview() {
        KnowledgeDocument document = createDocument("원문");

        assertThatThrownBy(document::publish)
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("검수자와 검수 시각이 있어야 문서를 공개할 수 있습니다.");
    }

    @Test
    @DisplayName("공식 출처 URL이 없는 문서는 검수 후에도 공개할 수 없다")
    void rejectsOfficialDocumentWithoutSourceUrl() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .topic(topic())
                .createdByMember(admin("등록자"))
                .title("제목")
                .sourceType(KnowledgeSourceType.OFFICIAL_DOC)
                .technologyVersion("Java 21")
                .licenseNote("인용 가능")
                .content("원문")
                .build();
        document.review(admin("검수자"));

        assertThatThrownBy(document::publish)
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("공식 출처 문서는 sourceUrl이 있어야 공개할 수 있습니다.");
    }

    @Test
    @DisplayName("HTTP 또는 HTTPS가 아닌 출처 URL로 문서를 만들 수 없다")
    void rejectsInvalidSourceUrl() {
        assertThatThrownBy(() -> KnowledgeDocument.builder()
                .topic(topic())
                .createdByMember(admin("등록자"))
                .title("제목")
                .sourceType(KnowledgeSourceType.OFFICIAL_DOC)
                .sourceUrl("ftp://example.com/docs")
                .technologyVersion("Java 21")
                .licenseNote("인용 가능")
                .content("원문")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceUrl must be a valid HTTP URL");
    }

    @Test
    @DisplayName("기술 버전이 없는 문서는 검수 후에도 공개할 수 없다")
    void rejectsPublicationWithoutTechnologyVersion() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .topic(topic())
                .createdByMember(admin("등록자"))
                .title("제목")
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .licenseNote("내부 작성")
                .content("원문")
                .build();
        document.review(admin("검수자"));

        assertThatThrownBy(document::publish)
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("technologyVersion이 있어야 문서를 공개할 수 있습니다.");
    }

    @Test
    @DisplayName("라이선스 메모가 없는 문서는 검수 후에도 공개할 수 없다")
    void rejectsPublicationWithoutLicenseNote() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .topic(topic())
                .createdByMember(admin("등록자"))
                .title("제목")
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("Java 21")
                .content("원문")
                .build();
        document.review(admin("검수자"));

        assertThatThrownBy(document::publish)
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("licenseNote가 있어야 문서를 공개할 수 있습니다.");
    }

    @Test
    @DisplayName("초안을 수정하면 이전 검수 정보를 무효화하고 checksum을 다시 계산한다")
    void draftUpdateInvalidatesReview() {
        KnowledgeDocument document = createDocument("기존 원문");
        document.review(admin("검수자"));

        document.updateDraft(
                topic(), "변경 제목", KnowledgeSourceType.INTERNAL_SUMMARY, null,
                "Spring Boot 4.1", "내부 작성", "변경 원문"
        );

        assertThat(document.getTitle()).isEqualTo("변경 제목");
        assertThat(document.getChecksum()).isEqualTo(ContentChecksum.sha256("변경 원문"));
        assertThat(document.getReviewedByMember()).isNull();
        assertThat(document.getReviewedAt()).isNull();
    }

    @Test
    @DisplayName("초안 수정 입력 검증이 실패하면 어떤 필드도 부분 수정하지 않는다")
    void failedDraftUpdateIsAtomic() {
        KnowledgeDocument document = createDocument("기존 원문");
        String checksum = document.getChecksum();
        LocalDateTime updatedAt = document.getUpdatedAt();

        assertThatThrownBy(() -> document.updateDraft(
                topic(), "변경 제목", KnowledgeSourceType.INTERNAL_SUMMARY, null,
                "Spring Boot 4.1", "내부 작성", "   "
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(document.getTitle()).isEqualTo("문서 제목");
        assertThat(document.getContent()).isEqualTo("기존 원문");
        assertThat(document.getChecksum()).isEqualTo(checksum);
        assertThat(document.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("PUBLISHED 문서는 직접 수정할 수 없고 같은 계열의 다음 DRAFT 버전을 만든다")
    void createsNextVersionWithoutOverwritingPublishedVersion() {
        KnowledgeDocument published = createDocument("기존 원문");
        published.review(admin("검수자"));
        published.publish();

        assertThatThrownBy(() -> published.updateDraft(
                topic(), "변경", KnowledgeSourceType.INTERNAL_SUMMARY, null,
                "Java 22", "내부", "변경 원문"
        )).isInstanceOf(InvalidContentStateException.class);

        KnowledgeDocument next = published.createNextVersion(
                2, topic(), admin("새 등록자"), "문서 제목", KnowledgeSourceType.OFFICIAL_DOC,
                "https://example.com/docs", "Java 22", "인용 가능", "새 원문"
        );

        assertThat(published.getStatus()).isEqualTo(KnowledgeDocumentStatus.PUBLISHED);
        assertThat(published.getContent()).isEqualTo("기존 원문");
        assertThat(next.getStatus()).isEqualTo(KnowledgeDocumentStatus.DRAFT);
        assertThat(next.getDocumentVersion()).isEqualTo(2);
        assertThat(next.getVersionSeriesId()).isEqualTo(published.getVersionSeriesId());
    }

    private KnowledgeDocument createDocument(String content) {
        return KnowledgeDocument.builder()
                .topic(topic())
                .createdByMember(admin("등록자"))
                .title("문서 제목")
                .sourceType(KnowledgeSourceType.OFFICIAL_DOC)
                .sourceUrl("https://example.com/docs")
                .technologyVersion("Java 21")
                .licenseNote("인용 가능")
                .content(content)
                .build();
    }

    private Topic topic() {
        return Topic.builder().code("JAVA").name("Java").build();
    }

    private Member admin(String nickname) {
        return Member.builder().nickname(nickname).role(MemberRole.ADMIN).build();
    }
}
