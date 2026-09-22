package com.example.crackcs.evaluation.retrieval;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeEvidenceSelectorTest {

    private final KnowledgeEvidenceSelector knowledgeEvidenceSelector = new KnowledgeEvidenceSelector();

    @Test
    @DisplayName("개념 일치와 검색어 일치에 가중치를 적용해 관련 근거만 점수화한다")
    void scoresOnlyRelevantEvidenceWithConceptWeight() {
        Topic topic = Topic.builder().code("CACHE").name("캐시").build();
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        KnowledgeDocument relevantDocument = createPublishedDocument(topic, admin, "관련 근거", "cache ttl expiry");
        KnowledgeDocument irrelevantDocument = createPublishedDocument(topic, admin, "무관한 근거", "network latency");
        KnowledgeChunk relevantChunk = KnowledgeChunk.create(relevantDocument, 0, 0, 16,
                "cache ttl expiry", "policy-v1");
        KnowledgeChunk irrelevantChunk = KnowledgeChunk.create(irrelevantDocument, 0, 0, 15,
                "network latency", "policy-v1");
        RetrievalQuery query = new RetrievalQuery(1L, List.of("cache"), "cache ttl", "ttl expiry", "ignored");

        RetrievalResult retrievalResult = knowledgeEvidenceSelector.selectEvidence(
                List.of(irrelevantChunk, relevantChunk), query, 5);

        assertThat(retrievalResult.chunks()).extracting(RetrievedChunk::chunk)
                .containsExactly(relevantChunk);
        assertThat(retrievalResult.chunks().getFirst().relevanceScore()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("반환 한도 안에서 각 개념을 지지하는 근거를 하나씩 보존한다")
    void preservesEvidenceForEachConcept() {
        Topic topic = Topic.builder().code("POLICY").name("정책").build();
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        KnowledgeDocument alphaDocument = createPublishedDocument(topic, admin, "Alpha 근거",
                "alpha shared second third fourth fifth sixth");
        KnowledgeDocument duplicateAlphaDocument = createPublishedDocument(topic, admin, "Alpha 중복 근거",
                "alpha shared second third fourth fifth");
        KnowledgeDocument betaDocument = createPublishedDocument(topic, admin, "Beta 근거", "beta shared second");
        KnowledgeChunk alphaChunk = KnowledgeChunk.create(alphaDocument, 0, 0,
                alphaDocument.getContent().length(), alphaDocument.getContent(), "policy-v1");
        KnowledgeChunk duplicateAlphaChunk = KnowledgeChunk.create(duplicateAlphaDocument, 0, 0,
                duplicateAlphaDocument.getContent().length(), duplicateAlphaDocument.getContent(), "policy-v1");
        KnowledgeChunk betaChunk = KnowledgeChunk.create(betaDocument, 0, 0,
                betaDocument.getContent().length(), betaDocument.getContent(), "policy-v1");
        RetrievalQuery query = new RetrievalQuery(1L, List.of("alpha", "beta"),
                "alpha beta shared second", "third fourth fifth sixth", "ignored");

        RetrievalResult retrievalResult = knowledgeEvidenceSelector.selectEvidence(
                List.of(duplicateAlphaChunk, betaChunk, alphaChunk), query, 2);

        assertThat(retrievalResult.chunks()).extracting(RetrievedChunk::chunk)
                .containsExactly(alphaChunk, betaChunk);
    }

    @Test
    @DisplayName("최고 점수의 절반보다 낮은 약한 후보는 반환 한도를 채우는 데 사용하지 않는다")
    void excludesEvidenceBelowRelativeScoreFloor() {
        Topic topic = Topic.builder().code("CACHE_FLOOR").name("캐시 점수 하한").build();
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        KnowledgeDocument strongDocument = createPublishedDocument(topic, admin, "강한 근거",
                "cache ttl expiry validation server response policy");
        KnowledgeDocument weakDocument = createPublishedDocument(topic, admin, "약한 근거", "cache");
        KnowledgeChunk strongChunk = KnowledgeChunk.create(strongDocument, 0, 0,
                strongDocument.getContent().length(), strongDocument.getContent(), "policy-v1");
        KnowledgeChunk weakChunk = KnowledgeChunk.create(weakDocument, 0, 0,
                weakDocument.getContent().length(), weakDocument.getContent(), "policy-v1");
        RetrievalQuery query = new RetrievalQuery(1L, List.of("cache"),
                "cache ttl expiry validation", "server response policy", "ignored");

        RetrievalResult retrievalResult = knowledgeEvidenceSelector.selectEvidence(
                List.of(weakChunk, strongChunk), query, 5);

        assertThat(retrievalResult.chunks()).extracting(RetrievedChunk::chunk)
                .containsExactly(strongChunk);
    }

    @Test
    @DisplayName("상대 점수가 낮아도 질문을 충분히 공유하는 짧은 반대 근거를 보존한다")
    void preservesConciseConflictingEvidence() {
        Topic topic = Topic.builder().code("CONFLICT").name("충돌 근거").build();
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        String detailedContent = "캐시 재검증은 서버 확인이 필요하고 조건부 요청은 변경 여부를 전달하며 응답 코드는 표현 재사용을 결정한다.";
        String conflictingContent = "캐시 재검증은 서버 확인이 필요 없다.";
        KnowledgeDocument detailedDocument = createPublishedDocument(topic, admin, "상세 근거", detailedContent);
        KnowledgeDocument conflictingDocument = createPublishedDocument(topic, admin, "반대 근거", conflictingContent);
        KnowledgeChunk detailedChunk = KnowledgeChunk.create(detailedDocument, 0, 0,
                detailedContent.length(), detailedContent, "policy-v1");
        KnowledgeChunk conflictingChunk = KnowledgeChunk.create(conflictingDocument, 0, 0,
                conflictingContent.length(), conflictingContent, "policy-v1");
        RetrievalQuery query = new RetrievalQuery(1L, List.of("캐시"),
                "캐시 재검증은 서버 확인이 필요한가?", detailedContent, "ignored");

        RetrievalResult retrievalResult = knowledgeEvidenceSelector.selectEvidence(
                List.of(conflictingChunk, detailedChunk), query, 2);

        assertThat(retrievalResult.chunks()).extracting(RetrievedChunk::chunk)
                .containsExactly(detailedChunk, conflictingChunk);
        assertThat(retrievalResult.conflictingEvidence()).isTrue();
    }

    @Test
    @DisplayName("점수가 같으면 문서와 Chunk 순번 기준으로 항상 같은 순서를 반환한다")
    void sortsEqualScoresByDocumentAndSequence() {
        Topic topic = Topic.builder().code("STABLE").name("안정 정렬").build();
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        String content = "cache ttl cache ttl";
        KnowledgeDocument document = createPublishedDocument(topic, admin, "동점 근거", content);
        KnowledgeChunk firstChunk = KnowledgeChunk.create(document, 0, 0, 9, "cache ttl", "policy-v1");
        KnowledgeChunk secondChunk = KnowledgeChunk.create(document, 1, 10, 19, "cache ttl", "policy-v1");
        RetrievalQuery query = new RetrievalQuery(1L, List.of("cache"), "cache ttl", "", "ignored");

        RetrievalResult retrievalResult = knowledgeEvidenceSelector.selectEvidence(
                List.of(secondChunk, firstChunk), query, 2);

        assertThat(retrievalResult.chunks()).extracting(RetrievedChunk::chunk)
                .containsExactly(firstChunk, secondChunk);
    }

    private KnowledgeDocument createPublishedDocument(Topic topic, Member admin, String title, String content) {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .topic(topic)
                .createdByMember(admin)
                .title(title)
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general")
                .licenseNote("독립 작성")
                .content(content)
                .build();
        document.review(admin);
        document.publish();
        return document;
    }
}
