package com.example.crackcs.content.knowledge.repository;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class KnowledgeDocumentRepositoryTest {

    @Autowired
    KnowledgeDocumentRepository documentRepository;
    @Autowired
    TopicRepository topicRepository;
    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("같은 checksum의 원문을 중복 저장할 수 없다")
    void rejectsDuplicateChecksum() {
        Topic topic = saveTopic();
        Member admin = saveAdmin();
        documentRepository.save(document(topic, admin, "같은 원문"));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(document(topic, admin, "같은 원문")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private KnowledgeDocument document(Topic topic, Member admin, String content) {
        return KnowledgeDocument.builder()
                .topic(topic).createdByMember(admin).title("문서")
                .sourceType(KnowledgeSourceType.OFFICIAL_DOC).sourceUrl("https://example.com")
                .technologyVersion("Java 21").licenseNote("인용 가능").content(content).build();
    }

    private Topic saveTopic() {
        return topicRepository.save(Topic.builder().code("JAVA").name("Java").build());
    }

    private Member saveAdmin() {
        return memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
    }
}
