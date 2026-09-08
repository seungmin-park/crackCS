package com.example.crackcs.content.knowledge.chunk.service;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class KnowledgeChunkServiceTest {

    @Autowired
    KnowledgeChunkService service;
    @Autowired
    KnowledgeChunkRepository chunks;
    @Autowired
    KnowledgeDocumentRepository documents;
    @Autowired
    TopicRepository topics;
    @Autowired
    MemberRepository members;

    @AfterEach
    void tearDown() {
        chunks.deleteAllInBatch();
        documents.deleteAllInBatch();
        topics.deleteAllInBatch();
        members.deleteAllInBatch();
    }

    @Test
    @DisplayName("공개 문서를 원문 순서가 보존된 검색 가능 Chunk로 생성한다")
    void chunksPublishedDocumentInSourceOrder() {
        KnowledgeDocument document = saveDocument("프로세스 설명.\n\n스레드 설명.", true);

        ChunkGenerationResult result = service.generateChunks(document.getId());

        assertThat(result.reused()).isFalse();
        assertThat(chunks.findAllByDocument_IdOrderBySequenceNo(document.getId()))
                .extracting(KnowledgeChunk::getContent)
                .containsExactly("프로세스 설명.", "스레드 설명.");
    }

    @Test
    @DisplayName("같은 공개 문서의 Chunk 생성 요청은 기존 근거 보존을 위해 결과를 재사용한다")
    void reusesSameChunkingJob() {
        KnowledgeDocument document = saveDocument("프로세스 설명.", true);
        service.generateChunks(document.getId());

        ChunkGenerationResult second = service.generateChunks(document.getId());

        assertThat(second.reused()).isTrue();
        assertThat(chunks.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("공개되지 않은 문서는 Chunk 생성 요청을 거부한다")
    void rejectsDraftDocument() {
        KnowledgeDocument document = saveDocument("초안", false);

        assertThatThrownBy(() -> service.generateChunks(document.getId()))
                .isInstanceOf(InvalidContentStateException.class);
    }

    private KnowledgeDocument saveDocument(String content, boolean publish) {
        Topic topic = topics.save(Topic.builder().code("OS" + System.nanoTime()).name("운영체제").build());
        Member admin = members.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        KnowledgeDocument document = documents.save(KnowledgeDocument.builder()
                .topic(topic).createdByMember(admin).title("운영체제")
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성").content(content).build());
        if (publish) {
            document.review(admin);
            document.publish();
            documents.save(document);
        }
        return document;
    }
}
