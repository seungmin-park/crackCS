package com.example.crackcs.content.knowledge.chunk.domain;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeChunkTest {

    @Test
    @DisplayName("공개 문서의 Chunk는 원문 위치와 검색 가능 상태를 보존한다")
    void createsSearchableChunkFromPublishedDocument() {
        KnowledgeDocument document = publishedDocument("프로세스는 실행 중인 프로그램이다.");

        KnowledgeChunk chunk = KnowledgeChunk.create(
                document, 0, 0, document.getContent().length(), document.getContent(), "policy-v1"
        );

        assertThat(chunk.getSequenceNo()).isZero();
        assertThat(chunk.getStartOffset()).isZero();
        assertThat(chunk.getEndOffset()).isEqualTo(document.getContent().length());
        assertThat(chunk.getSearchStatus()).isEqualTo(KnowledgeChunkSearchStatus.KEYWORD_SEARCHABLE);
        assertThat(chunk.getChecksum()).isNotBlank();
    }

    @Test
    @DisplayName("DRAFT 문서에서는 검색 Chunk를 만들 수 없다")
    void rejectsDraftDocument() {
        KnowledgeDocument draft = document("초안");

        assertThatThrownBy(() -> KnowledgeChunk.create(draft, 0, 0, 2, "초안", "policy-v1"))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("PUBLISHED 문서만 Chunk를 생성할 수 있습니다.");
    }

    private KnowledgeDocument publishedDocument(String content) {
        KnowledgeDocument document = document(content);
        document.review(admin("검수자"));
        document.publish();
        return document;
    }

    private KnowledgeDocument document(String content) {
        return KnowledgeDocument.builder()
                .topic(Topic.builder().code("OS").name("운영체제").build())
                .createdByMember(admin("등록자"))
                .title("운영체제")
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general")
                .licenseNote("독립 작성")
                .content(content)
                .build();
    }

    private Member admin(String nickname) {
        return Member.builder().nickname(nickname).role(MemberRole.ADMIN).build();
    }
}
