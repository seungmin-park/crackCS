package com.example.crackcs.content.knowledge.service;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.exception.DuplicateKnowledgeDocumentException;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class KnowledgeDocumentServiceTest {

    @Autowired
    KnowledgeDocumentService knowledgeDocumentService;
    @Autowired
    KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Autowired
    TopicRepository topicRepository;
    @Autowired
    MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        knowledgeDocumentRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("문서를 검수하고 공개한 뒤 기존 공개본을 보존하는 다음 버전을 생성한다")
    void preservesPublishedVersionWhenCreatingNextVersion() {
        Topic topic = saveTopic();
        Member contentAdmin = saveAdmin();
        KnowledgeDocument publishedDocument = knowledgeDocumentService.create(
                contentAdmin.getId(), knowledgeDocumentDraft(topic, "첫 버전 원문"));
        knowledgeDocumentService.review(publishedDocument.getId(), contentAdmin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(publishedDocument.getId());

        KnowledgeDocument nextDocumentVersion = knowledgeDocumentService.createNextVersion(
                publishedDocument.getId(), contentAdmin.getId(), knowledgeDocumentDraft(topic, "둘째 버전 원문")
        );

        assertThat(nextDocumentVersion.getDocumentVersion()).isEqualTo(2);
        assertThat(nextDocumentVersion.getStatus()).isEqualTo(KnowledgeDocumentStatus.DRAFT);
        assertThat(nextDocumentVersion.getVersionSeriesId()).isEqualTo(publishedDocument.getVersionSeriesId());
        assertThat(knowledgeDocumentRepository.findById(publishedDocument.getId()).orElseThrow().getContent())
                .isEqualTo("첫 버전 원문");
        assertThat(knowledgeDocumentRepository.findAll()).hasSize(2);

        knowledgeDocumentService.review(nextDocumentVersion.getId(), contentAdmin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(nextDocumentVersion.getId());

        assertThat(knowledgeDocumentRepository.findById(publishedDocument.getId()).orElseThrow().getStatus())
                .isEqualTo(KnowledgeDocumentStatus.RETIRED);
        assertThat(knowledgeDocumentRepository.findById(nextDocumentVersion.getId()).orElseThrow().getStatus())
                .isEqualTo(KnowledgeDocumentStatus.PUBLISHED);
    }

    @Test
    @DisplayName("새 문서 버전 공개에 실패하면 이전 공개본과 새 초안 상태를 모두 유지한다")
    void preservesVersionStatusesWhenPublishingNextVersionFails() {
        Topic topic = saveTopic();
        Member contentAdmin = saveAdmin();
        KnowledgeDocument publishedDocument = knowledgeDocumentService.create(
                contentAdmin.getId(), knowledgeDocumentDraft(topic, "첫 버전 원문"));
        knowledgeDocumentService.review(publishedDocument.getId(), contentAdmin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(publishedDocument.getId());
        KnowledgeDocument unreviewedNextVersion = knowledgeDocumentService.createNextVersion(
                publishedDocument.getId(), contentAdmin.getId(), knowledgeDocumentDraft(topic, "둘째 버전 원문")
        );

        assertThatThrownBy(() -> knowledgeDocumentService.publishAsCurrentVersion(unreviewedNextVersion.getId()))
                .isInstanceOf(InvalidContentStateException.class);

        assertThat(knowledgeDocumentRepository.findById(publishedDocument.getId()).orElseThrow().getStatus())
                .isEqualTo(KnowledgeDocumentStatus.PUBLISHED);
        assertThat(knowledgeDocumentRepository.findById(unreviewedNextVersion.getId()).orElseThrow().getStatus())
                .isEqualTo(KnowledgeDocumentStatus.DRAFT);
    }

    @Test
    @DisplayName("줄바꿈만 다른 동일 원문을 중복 등록할 수 없다")
    void detectsDuplicateNormalizedContent() {
        Topic topic = saveTopic();
        Member contentAdmin = saveAdmin();
        knowledgeDocumentService.create(contentAdmin.getId(), knowledgeDocumentDraft(topic, "첫 줄\r\n둘째 줄"));

        assertThatThrownBy(() -> knowledgeDocumentService.create(
                contentAdmin.getId(), knowledgeDocumentDraft(topic, "첫 줄\n둘째 줄")))
                .isInstanceOf(DuplicateKnowledgeDocumentException.class);
    }

    @Test
    @DisplayName("비활성 Topic에는 새 문서를 연결할 수 없다")
    void rejectsInactiveTopicConnection() {
        Topic topic = saveTopic();
        Member contentAdmin = saveAdmin();
        topic.deactivate();
        topicRepository.save(topic);

        assertThatThrownBy(() -> knowledgeDocumentService.create(
                contentAdmin.getId(), knowledgeDocumentDraft(topic, "원문")))
                .isInstanceOf(InvalidContentStateException.class)
                .hasMessage("비활성 Topic에는 KnowledgeDocument를 연결할 수 없습니다.");
    }

    private KnowledgeDocumentDraft knowledgeDocumentDraft(Topic topic, String content) {
        return new KnowledgeDocumentDraft(
                topic.getId(), "문서", KnowledgeSourceType.OFFICIAL_DOC, "https://example.com/docs",
                "Java 21", "인용 가능", content
        );
    }

    private Topic saveTopic() {
        return topicRepository.save(Topic.builder().code("JAVA").name("Java").build());
    }

    private Member saveAdmin() {
        return memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
    }
}
