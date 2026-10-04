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

    @Test
    @DisplayName("fork와 exec 설명의 부정문을 파일 디스크립터 설명과 충돌로 오인하지 않는다")
    void doesNotTreatComplementaryProcessEvidenceAsConflict() {
        Topic topic = Topic.builder().code("PROCESS").name("운영체제").build();
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        String processContent = "[OS-101] fork의 프로세스 생성과 exec의 프로그램 교체는 별개의 동작이다. "
                + "fork가 성공하면 부모와 새 자식이 각각 실행을 이어간다. "
                + "exec가 성공하면 호출한 자식 프로세스의 프로그램 이미지가 교체되고 이전 코드로 돌아오지 않는다. "
                + "exec 자체가 새 프로세스를 하나 더 만드는 것은 아니다. "
                + "근거: OS-API — 5.1 fork, 5.3 exec; OS-XV6 — 1.1 Processes and memory";
        String descriptorContent = "[OS-103] 프로세스별 파일 디스크립터 테이블은 별개다. "
                + "일반 파일을 연 뒤 fork로 상속한 디스크립터는 같은 열린 파일 상태를 참조하므로 파일 오프셋을 공유한다. "
                + "한쪽의 읽기는 다른 쪽이 다음에 읽을 위치에도 영향을 준다. "
                + "자식이 자신의 디스크립터를 close해도 부모의 참조는 유지된다. "
                + "부모와 자식이 파일을 각각 다시 연 경우는 이 상속 사례와 구분한다. "
                + "근거: OS-XV6 — 1.2 I/O and File descriptors, pp. 13–15; "
                + "OS-API — 5.4 file descriptors; homework 2 (상속한 디스크립터의 입출력 사례)";
        KnowledgeDocument processDocument = createPublishedDocument(topic, admin, "프로세스", processContent);
        KnowledgeDocument descriptorDocument = createPublishedDocument(topic, admin, "파일", descriptorContent);
        KnowledgeChunk processChunk = KnowledgeChunk.create(processDocument, 0, 0,
                processContent.length(), processContent, "policy-v1");
        KnowledgeChunk descriptorChunk = KnowledgeChunk.create(descriptorDocument, 0, 0,
                descriptorContent.length(), descriptorContent, "policy-v1");
        RetrievalQuery query = new RetrievalQuery(1L,
                List.of("fork의 프로세스 생성"),
                "그렇다면 셸이 외부 명령을 실행할 때, 부모 셸은 계속 살아서 다음 명령을 받을 수 있으면서 "
                        + "자식만 새 프로그램을 실행하게 하려면 fork()와 exec()를 각각 어느 프로세스가 호출해야 하나요?",
                "셸(부모 프로세스)이 fork()를 호출해 자식을 만든다. "
                        + "তারপর 자식 프로세스가 exec()를 호출해 자신의 프로그램 이미지를 외부 명령 프로그램으로 교체한다. "
                        + "부모 셸은 exec()하지 않고 계속 실행되므로 다음 명령을 받을 수 있다.",
                "");

        RetrievalResult retrievalResult = knowledgeEvidenceSelector.selectEvidence(
                List.of(processChunk, descriptorChunk), query, 5);

        assertThat(retrievalResult.chunks()).extracting(RetrievedChunk::chunk)
                .containsExactlyInAnyOrder(processChunk, descriptorChunk);
        assertThat(retrievalResult.conflictingEvidence()).isFalse();
    }

    @Test
    @DisplayName("문단에 다른 부정문이 있어도 같은 주장의 긍정과 부정은 충돌로 표시한다")
    void detectsOpposingStatementsInsideMixedPolarityParagraphs() {
        Topic topic = Topic.builder().code("CACHE_STATEMENT").name("캐시").build();
        Member admin = Member.builder().nickname("관리자").role(MemberRole.ADMIN).build();
        String positiveContent = "캐시 재검증은 서버 확인이 필요하다. 로컬 만료는 네트워크 요청이 필요 없다.";
        String negativeContent = "캐시 재검증은 서버 확인이 필요 없다.";
        KnowledgeDocument positiveDocument = createPublishedDocument(topic, admin, "긍정", positiveContent);
        KnowledgeDocument negativeDocument = createPublishedDocument(topic, admin, "부정", negativeContent);
        KnowledgeChunk positiveChunk = KnowledgeChunk.create(positiveDocument, 0, 0,
                positiveContent.length(), positiveContent, "policy-v1");
        KnowledgeChunk negativeChunk = KnowledgeChunk.create(negativeDocument, 0, 0,
                negativeContent.length(), negativeContent, "policy-v1");
        RetrievalQuery query = new RetrievalQuery(1L, List.of("캐시"),
                "캐시 재검증은 서버 확인이 필요한가?", positiveContent, "");

        RetrievalResult retrievalResult = knowledgeEvidenceSelector.selectEvidence(
                List.of(positiveChunk, negativeChunk), query, 5);

        assertThat(retrievalResult.conflictingEvidence()).isTrue();
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
