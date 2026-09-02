package com.example.crackcs.content.knowledge.service;

import com.example.crackcs.content.knowledge.domain.ContentChecksum;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.exception.DuplicateKnowledgeDocumentException;
import com.example.crackcs.exception.InvalidContentStateException;
import com.example.crackcs.exception.KnowledgeDocumentNotFoundException;
import com.example.crackcs.exception.MemberNotFoundException;
import com.example.crackcs.exception.TopicNotFoundException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultKnowledgeDocumentService implements KnowledgeDocumentService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final TopicRepository topicRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public KnowledgeDocument create(Long creatorMemberId, KnowledgeDocumentData data) {
        Topic topic = findActiveTopic(data.topicId());
        Member creator = findAdmin(creatorMemberId);
        ensureUniqueContent(data.content(), null);
        return knowledgeDocumentRepository.save(KnowledgeDocument.builder()
                .topic(topic)
                .createdByMember(creator)
                .title(data.title())
                .sourceType(data.sourceType())
                .sourceUrl(data.sourceUrl())
                .technologyVersion(data.technologyVersion())
                .licenseNote(data.licenseNote())
                .content(data.content())
                .build());
    }

    @Override
    public Page<KnowledgeDocument> findAll(
            Long topicId,
            KnowledgeDocumentStatus status,
            String technologyVersion,
            Pageable pageable
    ) {
        return knowledgeDocumentRepository.findAllByConditions(
                topicId, status, technologyVersion, pageable
        );
    }

    @Override
    public KnowledgeDocument findById(Long documentId) {
        return findDocument(documentId);
    }

    @Override
    @Transactional
    public KnowledgeDocument update(Long documentId, KnowledgeDocumentData data) {
        KnowledgeDocument document = findDocument(documentId);
        Topic topic = findActiveTopic(data.topicId());
        ensureUniqueContent(data.content(), documentId);
        document.updateDraft(
                topic,
                data.title(),
                data.sourceType(),
                data.sourceUrl(),
                data.technologyVersion(),
                data.licenseNote(),
                data.content()
        );
        return document;
    }

    @Override
    @Transactional
    public KnowledgeDocument createNextVersion(
            Long documentId,
            Long creatorMemberId,
            KnowledgeDocumentData data
    ) {
        KnowledgeDocument source = findDocument(documentId);
        Topic topic = findActiveTopic(data.topicId());
        Member creator = findAdmin(creatorMemberId);
        ensureUniqueContent(data.content(), null);
        int nextVersion = knowledgeDocumentRepository.findMaxVersion(source.getVersionSeriesId()) + 1;
        return knowledgeDocumentRepository.save(source.createNextVersion(
                nextVersion,
                topic,
                creator,
                data.title(),
                data.sourceType(),
                data.sourceUrl(),
                data.technologyVersion(),
                data.licenseNote(),
                data.content()
        ));
    }

    @Override
    @Transactional
    public KnowledgeDocument review(Long documentId, Long reviewerMemberId) {
        KnowledgeDocument document = findDocument(documentId);
        document.review(findAdmin(reviewerMemberId));
        return document;
    }

    @Override
    @Transactional
    public KnowledgeDocument publish(Long documentId) {
        KnowledgeDocument document = findDocument(documentId);
        document.publish();
        knowledgeDocumentRepository.findAllByVersionSeriesIdAndStatus(
                        document.getVersionSeriesId(), KnowledgeDocumentStatus.PUBLISHED
                ).stream()
                .filter(previous -> !previous.getId().equals(document.getId()))
                .forEach(KnowledgeDocument::retire);
        return document;
    }

    @Override
    @Transactional
    public KnowledgeDocument retire(Long documentId) {
        KnowledgeDocument document = findDocument(documentId);
        document.retire();
        return document;
    }

    @Override
    public List<KnowledgeDocument> findPublishedCandidates(Long topicId) {
        return knowledgeDocumentRepository.findPublishedCandidatesByTopicId(topicId);
    }

    private void ensureUniqueContent(String content, Long currentId) {
        String normalizedContent = content == null
                ? ""
                : content.replace("\r\n", "\n").replace('\r', '\n').strip();
        String checksum = ContentChecksum.sha256(normalizedContent);
        boolean duplicated = currentId == null
                ? knowledgeDocumentRepository.existsByChecksum(checksum)
                : knowledgeDocumentRepository.existsByChecksumAndIdNot(checksum, currentId);
        if (duplicated) {
            throw new DuplicateKnowledgeDocumentException();
        }
    }

    private Topic findActiveTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));
        if (!topic.isActive()) {
            throw new InvalidContentStateException("비활성 Topic에는 KnowledgeDocument를 연결할 수 없습니다.");
        }
        return topic;
    }

    private Member findAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (member.getRole() != MemberRole.ADMIN || !member.isAuthenticatable()) {
            throw new InvalidContentStateException("활성 ADMIN 회원만 콘텐츠를 등록하거나 검수할 수 있습니다.");
        }
        return member;
    }

    private KnowledgeDocument findDocument(Long documentId) {
        return knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(documentId));
    }
}
