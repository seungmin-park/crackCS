package com.example.crackcs.content.knowledge.service;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentContent;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.exception.*;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultKnowledgeDocumentService implements KnowledgeDocumentService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final TopicRepository topicRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public KnowledgeDocument create(Long creatorMemberId, KnowledgeDocumentDraft draft) {
        Topic topic = findActiveTopic(draft.topicId());
        Member creator = findAdmin(creatorMemberId);
        KnowledgeDocumentContent documentContent = KnowledgeDocumentContent.from(draft.content());
        ensureUniqueChecksum(documentContent.checksum(), null);
        return knowledgeDocumentRepository.save(KnowledgeDocument.builder()
                .topic(topic)
                .createdByMember(creator)
                .title(draft.title())
                .sourceType(draft.sourceType())
                .sourceUrl(draft.sourceUrl())
                .technologyVersion(draft.technologyVersion())
                .licenseNote(draft.licenseNote())
                .content(documentContent.value())
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
    public KnowledgeDocument update(Long documentId, KnowledgeDocumentDraft draft) {
        KnowledgeDocument document = findDocumentWithVersionSeriesLock(documentId);
        Topic topic = findActiveTopic(draft.topicId());
        KnowledgeDocumentContent documentContent = KnowledgeDocumentContent.from(draft.content());
        ensureUniqueChecksum(documentContent.checksum(), documentId);
        document.updateDraft(
                topic,
                draft.title(),
                draft.sourceType(),
                draft.sourceUrl(),
                draft.technologyVersion(),
                draft.licenseNote(),
                documentContent.value()
        );
        return document;
    }

    @Override
    @Transactional
    public KnowledgeDocument createNextVersion(
            Long documentId,
            Long creatorMemberId,
            KnowledgeDocumentDraft draft
    ) {
        KnowledgeDocument source = findDocumentWithVersionSeriesLock(documentId);
        Topic topic = findActiveTopic(draft.topicId());
        Member creator = findAdmin(creatorMemberId);
        KnowledgeDocumentContent documentContent = KnowledgeDocumentContent.from(draft.content());
        ensureUniqueChecksum(documentContent.checksum(), null);
        int nextVersion = knowledgeDocumentRepository.findMaxVersion(source.getVersionSeriesId()) + 1;
        return knowledgeDocumentRepository.save(source.createNextVersion(
                nextVersion,
                topic,
                creator,
                draft.title(),
                draft.sourceType(),
                draft.sourceUrl(),
                draft.technologyVersion(),
                draft.licenseNote(),
                documentContent.value()
        ));
    }

    @Override
    @Transactional
    public KnowledgeDocument review(Long documentId, Long reviewerMemberId) {
        KnowledgeDocument document = findDocumentWithVersionSeriesLock(documentId);
        document.review(findAdmin(reviewerMemberId));
        return document;
    }

    @Override
    @Transactional
    public KnowledgeDocument publishAsCurrentVersion(Long documentId) {
        KnowledgeDocument document = findDocumentWithVersionSeriesLock(documentId);
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
        KnowledgeDocument document = findDocumentWithVersionSeriesLock(documentId);
        document.retire();
        return document;
    }

    private void ensureUniqueChecksum(String checksum, Long currentId) {
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

    private KnowledgeDocument findDocumentWithVersionSeriesLock(Long documentId) {
        String versionSeriesId = knowledgeDocumentRepository.findVersionSeriesIdById(documentId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(documentId));
        // Retired first versions remain the shared lock target for every version in this series.
        knowledgeDocumentRepository.findFirstByVersionSeriesIdOrderByDocumentVersionAsc(versionSeriesId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(documentId));
        return findDocument(documentId);
    }

    private KnowledgeDocument findDocument(Long documentId) {
        return knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new KnowledgeDocumentNotFoundException(documentId));
    }
}
