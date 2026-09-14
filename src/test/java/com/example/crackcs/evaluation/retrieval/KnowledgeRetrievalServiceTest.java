package com.example.crackcs.evaluation.retrieval;

import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkService;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KnowledgeRetrievalServiceTest {

    @Autowired
    KnowledgeRetrievalService retrieval;
    @Autowired
    KnowledgeChunkService chunkService;
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
    @DisplayName("같은 Topic과 Concept에 관련된 공개 Chunk를 점수와 안정된 순서로 반환한다")
    void retrievesRelevantPublishedChunksDeterministically() {
        Topic operatingSystem = topics.save(Topic.builder().code("OS").name("운영체제").build());
        Topic database = topics.save(Topic.builder().code("DB").name("데이터베이스").build());
        Member admin = members.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        publishAndChunk(operatingSystem, admin, "프로세스", "프로세스는 독립된 주소 공간과 자원을 소유한다.");
        publishAndChunk(operatingSystem, admin, "스케줄링", "스케줄러는 실행할 프로세스를 선택한다.");
        publishAndChunk(database, admin, "트랜잭션", "트랜잭션은 원자성을 보장한다.");

        RetrievalResult first = retrieval.retrieve(new RetrievalQuery(
                operatingSystem.getId(), List.of("프로세스"),
                "프로세스와 스레드의 차이는?", "프로세스는 자원을 소유한다.", "프로세스는 독립적이다."
        ), 2);
        RetrievalResult second = retrieval.retrieve(new RetrievalQuery(
                operatingSystem.getId(), List.of("프로세스"),
                "프로세스와 스레드의 차이는?", "프로세스는 자원을 소유한다.", "프로세스는 독립적이다."
        ), 2);

        assertThat(first.chunks()).isNotEmpty();
        assertThat(first.chunks()).extracting(chunk -> chunk.chunk().getDocument().getTopic().getId())
                .containsOnly(operatingSystem.getId());
        assertThat(first.chunks()).extracting(chunk -> chunk.chunk().getId())
                .containsExactlyElementsOf(second.chunks().stream().map(chunk -> chunk.chunk().getId()).toList());
        assertThat(first.insufficientEvidence()).isFalse();
    }

    @Test
    @DisplayName("폐기된 문서의 Chunk와 관련 점수가 없는 Chunk를 근거에서 제외한다")
    void excludesRetiredAndIrrelevantChunks() {
        Topic topic = topics.save(Topic.builder().code("OS2").name("운영체제").build());
        Member admin = members.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        KnowledgeDocument retired = publishAndChunk(topic, admin, "이전", "프로세스는 자원을 소유한다.");
        retired.retire();
        documents.save(retired);
        publishAndChunk(topic, admin, "무관", "파일 시스템은 디렉터리를 관리한다.");

        RetrievalResult result = retrieval.retrieve(new RetrievalQuery(
                topic.getId(), List.of("프로세스"), "프로세스란?", "주소 공간", "모르겠습니다."
        ), 5);

        assertThat(result.chunks()).isEmpty();
        assertThat(result.insufficientEvidence()).isTrue();
    }

    private KnowledgeDocument publishAndChunk(Topic topic, Member admin, String title, String content) {
        KnowledgeDocument document = documents.save(KnowledgeDocument.builder()
                .topic(topic).createdByMember(admin).title(title)
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성").content(content).build());
        document.review(admin);
        document.publish();
        documents.save(document);
        chunkService.generateChunks(document.getId());
        return document;
    }
}
