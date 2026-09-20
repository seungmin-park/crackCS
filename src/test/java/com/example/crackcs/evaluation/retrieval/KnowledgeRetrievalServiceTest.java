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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class KnowledgeRetrievalServiceTest {

    @Autowired
    KnowledgeRetrievalService knowledgeRetrievalService;
    @Autowired
    KnowledgeChunkService knowledgeChunkService;
    @Autowired
    KnowledgeChunkRepository knowledgeChunkRepository;
    @Autowired
    KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Autowired
    TopicRepository topicRepository;
    @Autowired
    MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        knowledgeChunkRepository.deleteAllInBatch();
        knowledgeDocumentRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 20})
    @DisplayName("검색 근거 개수의 최소값과 최대값을 허용한다")
    void acceptsEvidenceLimitBoundaries(int limit) {
        RetrievalQuery query = new RetrievalQuery(1L, List.of(), "질문", "기준 답안", "사용자 답안");

        assertThatCode(() -> knowledgeRetrievalService.retrieve(query, limit))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 21})
    @DisplayName("허용 범위를 벗어난 검색 근거 개수를 거부한다")
    void rejectsEvidenceLimitOutsideAllowedRange(int limit) {
        RetrievalQuery query = new RetrievalQuery(1L, List.of(), "질문", "기준 답안", "사용자 답안");

        assertThatThrownBy(() -> knowledgeRetrievalService.retrieve(query, limit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("evidence limit must be between 1 and 20");
    }

    @Test
    @DisplayName("같은 Topic과 Concept에 관련된 공개 Chunk를 점수와 안정된 순서로 반환한다")
    void retrievesRelevantPublishedChunksDeterministically() {
        Topic operatingSystem = topicRepository.save(Topic.builder().code("OS").name("운영체제").build());
        Topic database = topicRepository.save(Topic.builder().code("DB").name("데이터베이스").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        publishAndChunk(operatingSystem, admin, "프로세스", "프로세스는 독립된 주소 공간과 자원을 소유한다.");
        publishAndChunk(operatingSystem, admin, "스케줄링", "스케줄러는 실행할 프로세스를 선택한다.");
        publishAndChunk(database, admin, "트랜잭션", "트랜잭션은 원자성을 보장한다.");

        RetrievalResult first = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                operatingSystem.getId(), List.of("프로세스"),
                "프로세스와 스레드의 차이는?", "프로세스는 자원을 소유한다.", "프로세스는 독립적이다."
        ), 2);
        RetrievalResult second = knowledgeRetrievalService.retrieve(new RetrievalQuery(
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
        Topic topic = topicRepository.save(Topic.builder().code("OS2").name("운영체제").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        KnowledgeDocument retired = publishAndChunk(topic, admin, "이전", "프로세스는 자원을 소유한다.");
        retired.retire();
        knowledgeDocumentRepository.save(retired);
        publishAndChunk(topic, admin, "무관", "파일 시스템은 디렉터리를 관리한다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("프로세스"), "프로세스란?", "주소 공간", "모르겠습니다."
        ), 5);

        assertThat(result.chunks()).isEmpty();
        assertThat(result.insufficientEvidence()).isTrue();
    }

    @Test
    @DisplayName("개념명 표현이 본문에 없어도 질문과 기준답안에 맞는 근거를 찾는다")
    void retrievesEvidenceWithoutLiteralConceptMatch() {
        Topic topic = topicRepository.save(Topic.builder().code("TRAFFIC").name("트래픽 제어").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        KnowledgeDocument evidence = publishAndChunk(topic, admin, "요청량 제한",
                "토큰 버킷은 초당 요청량과 순간 허용량을 제한한다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("부하 제어"), "요청 폭주를 어떻게 완화하는가?",
                "토큰 버킷은 초당 요청량과 순간 허용량을 제한한다.", "잘 모르겠습니다."
        ), 5);

        assertThat(result.chunks()).extracting(row -> row.chunk().getDocumentId())
                .containsExactly(evidence.getId());
        assertThat(result.insufficientEvidence()).isFalse();
    }

    @Test
    @DisplayName("강한 근거가 있으면 개념 단어만 겹치는 약한 후보로 결과를 채우지 않는다")
    void excludesWeakMatchesRatherThanFillingLimit() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        KnowledgeDocument evidence = publishAndChunk(topic, admin, "캐시 검증",
                "캐시 TTL 만료는 유효기간을 제한하고 ETag 재검증은 변경 여부를 확인한다.");
        publishAndChunk(topic, admin, "캐시 비용", "캐시 서버의 운영 비용은 장비 가격에 따라 달라진다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("캐시"), "캐시 만료와 재검증의 차이는?",
                "TTL 만료는 유효기간을 제한하고 ETag 재검증은 변경 여부를 확인한다.", "만료되면 다시 요청한다."
        ), 5);

        assertThat(result.chunks()).extracting(row -> row.chunk().getDocumentId())
                .containsExactly(evidence.getId());
    }

    @Test
    @DisplayName("제출 답안에만 등장하는 무관한 내용은 검색 근거가 되지 않는다")
    void doesNotUseAnswerOnlyTermsAsEvidence() {
        Topic topic = topicRepository.save(Topic.builder().code("STORAGE").name("저장소").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        publishAndChunk(topic, admin, "장비", "서버 장비 가격과 구매 비용");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("백업"), "스냅샷 복구", "데이터 시점 보존",
                "서버 장비 가격과 구매 비용"
        ), 5);

        assertThat(result.chunks()).isEmpty();
        assertThat(result.insufficientEvidence()).isTrue();
    }

    @Test
    @DisplayName("여러 개념의 근거가 별도 문서에 있으면 필요한 문서를 함께 반환한다")
    void retainsSeparateEvidenceForMultipleConcepts() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE2").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        KnowledgeDocument expiration = publishAndChunk(topic, admin, "만료", "TTL 만료는 유효기간을 제한한다.");
        KnowledgeDocument validation = publishAndChunk(topic, admin, "재검증", "ETag 재검증은 변경 여부를 확인한다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("TTL", "ETag"), "캐시 정책의 차이는?",
                "TTL 만료는 유효기간을 제한한다. ETag 재검증은 변경 여부를 확인한다.", "모르겠습니다."
        ), 5);

        assertThat(result.chunks()).extracting(row -> row.chunk().getDocumentId())
                .containsExactlyInAnyOrder(expiration.getId(), validation.getId());
    }

    @Test
    @DisplayName("긴 오답을 제출해도 질문의 근거 순위와 선택이 바뀌지 않는다")
    void keepsEvidenceStableWhenAnswerContainsDistractingTerms() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE3").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        KnowledgeDocument evidence = publishAndChunk(topic, admin, "캐시 검증",
                "캐시 TTL 만료는 유효기간을 제한하고 ETag 재검증은 변경 여부를 확인한다.");
        String distractingAnswer = "캐시 서버의 구매 비용과 장비 가격은 운영 인력과 유지 보수 계약과 전력 소비와 "
                + "공간 임대와 제조 업체와 물류 운송과 보험 요율에 따라 달라진다.";
        publishAndChunk(topic, admin, "장비 비용", distractingAnswer);

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("캐시"), "캐시 만료와 재검증의 차이는?",
                "TTL 만료는 유효기간을 제한하고 ETag 재검증은 변경 여부를 확인한다.", distractingAnswer
        ), 1);

        assertThat(result.chunks()).extracting(row -> row.chunk().getDocumentId())
                .containsExactly(evidence.getId());
    }

    @Test
    @DisplayName("관련 근거 사이의 명시적인 부정 표현 차이는 계속 충돌로 표시한다")
    void retainsConflictSignalBetweenRelevantEvidence() {
        Topic topic = topicRepository.save(Topic.builder().code("VALIDATION").name("검증").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        publishAndChunk(topic, admin, "정책 A", "캐시 재검증은 서버 확인이 필요하다.");
        publishAndChunk(topic, admin, "정책 B", "캐시 재검증은 서버 확인이 필요 없다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("캐시"), "캐시 재검증은 서버 확인이 필요한가?",
                "캐시 재검증은 서버 확인이 필요하다.", "모르겠습니다."
        ), 5);

        assertThat(result.chunks()).hasSize(2);
        assertThat(result.conflictingEvidence()).isTrue();
    }

    @Test
    @DisplayName("긴 근거보다 단어 점수가 낮아도 다른 필수 개념의 짧은 근거를 보존한다")
    void retainsShortEvidenceForAnotherConcept() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE4").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        String expirationText = "TTL 만료는 지정 시간이 지나면 저장 항목을 제거하는 정책으로 "
                + "유효기간 설정과 데이터 신선도 보장과 서버 부하 감소와 저장 공간 회수를 다룬다.";
        KnowledgeDocument expiration = publishAndChunk(topic, admin, "만료", expirationText);
        KnowledgeDocument validation = publishAndChunk(topic, admin, "재검증", "ETag 재검증은 변경 여부를 확인한다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("TTL", "ETag"), "TTL 만료와 ETag 재검증을 비교하세요.",
                expirationText + " ETag 재검증은 변경 여부를 확인한다.", "모르겠습니다."
        ), 5);

        assertThat(result.chunks()).extracting(row -> row.chunk().getDocumentId())
                .containsExactlyInAnyOrder(expiration.getId(), validation.getId());
    }

    @Test
    @DisplayName("개념명 일치 없이 질문의 일반 단어 하나만 겹치면 근거 부족으로 처리한다")
    void rejectsAnIsolatedQuestionWordWithoutConceptMatch() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE5").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        publishAndChunk(topic, admin, "비용", "다음 단계는 서버 비용을 산정하는 작업이다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("캐시"), "다음 설명에서 캐시 정책을 비교하세요.",
                "TTL 만료는 유효기간을 제한한다.", "모르겠습니다."
        ), 5);

        assertThat(result.chunks()).isEmpty();
        assertThat(result.insufficientEvidence()).isTrue();
    }

    @Test
    @DisplayName("반환 한도 안에서는 같은 개념의 중복 근거보다 다른 개념의 근거를 우선 보존한다")
    void reservesConceptEvidenceBeforeFillingLimit() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE6").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        String expirationText = "TTL 만료는 지정 시간이 지나면 저장 항목을 제거하는 정책으로 "
                + "유효기간 설정과 데이터 신선도 보장과 서버 부하 감소와 저장 공간 회수를 다룬다.";
        KnowledgeDocument expiration = publishAndChunk(topic, admin, "만료", expirationText);
        publishAndChunk(topic, admin, "만료의 다른 출처", expirationText + " 별도 출처의 설명이다.");
        KnowledgeDocument validation = publishAndChunk(topic, admin, "재검증", "ETag 재검증은 변경 여부를 확인한다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("TTL", "ETag"), "TTL 만료와 ETag 재검증을 비교하세요.",
                expirationText + " ETag 재검증은 변경 여부를 확인한다.", "모르겠습니다."
        ), 2);

        assertThat(result.chunks()).extracting(row -> row.chunk().getDocumentId())
                .containsExactly(expiration.getId(), validation.getId());
    }

    @Test
    @DisplayName("상세한 긍정 근거보다 점수가 낮은 짧은 반대 근거도 충돌 감지에서 누락하지 않는다")
    void retainsConciseContradictionAgainstDetailedEvidence() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE7").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        String detailedEvidence = "캐시 재검증은 서버 확인이 필요하고 조건부 요청은 변경 여부를 전달하며 "
                + "응답 코드는 저장된 표현을 재사용할지 결정한다.";
        publishAndChunk(topic, admin, "상세 근거", detailedEvidence);
        publishAndChunk(topic, admin, "반대 근거", "캐시 재검증은 서버 확인이 필요 없다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("캐시"), "캐시 재검증은 서버 확인이 필요한가?",
                detailedEvidence, "모르겠습니다."
        ), 2);

        assertThat(result.conflictingEvidence()).isTrue();
        assertThat(result.chunks()).hasSize(2);
    }

    @Test
    @DisplayName("다른 개념 이름과 일반 단어 하나만 겹치는 약한 후보는 보존하지 않는다")
    void doesNotReserveWeakNameOnlyEvidenceForAnotherConcept() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE8").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        String expirationText = "TTL 만료는 지정 시간이 지나면 저장 항목을 제거하는 정책으로 "
                + "유효기간 설정과 데이터 신선도 보장과 서버 부하 감소와 저장 공간 회수를 다룬다.";
        KnowledgeDocument expiration = publishAndChunk(topic, admin, "만료", expirationText);
        publishAndChunk(topic, admin, "비용", "ETag 다음 단계의 비용 산정");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("TTL", "ETag"), "다음 설명에서 TTL과 ETag를 비교하세요.",
                expirationText + " ETag 재검증은 변경 여부를 확인한다.", "모르겠습니다."
        ), 5);

        assertThat(result.chunks()).extracting(row -> row.chunk().getDocumentId())
                .containsExactly(expiration.getId());
    }

    @Test
    @DisplayName("문단 대부분이 다른 내용이면 공통 단어와 부정 표현만으로 반대 근거를 추가하지 않는다")
    void doesNotRescueAnUnrelatedNegativeParagraph() {
        Topic topic = topicRepository.save(Topic.builder().code("CACHE9").name("캐시").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        String reference = "캐시 재검증은 서버 확인이 필요하고 조건부 요청은 변경 여부를 전달하며 "
                + "응답 코드는 저장된 표현을 재사용할지 결정한다.";
        KnowledgeDocument evidence = publishAndChunk(topic, admin, "캐시 검증", reference);
        publishAndChunk(topic, admin, "장비 구매",
                "서버 확인이 필요 없다. 신규 장비의 구매 비용과 임대 장소와 전력 설비와 보험 계약을 따로 검토한다.");

        RetrievalResult result = knowledgeRetrievalService.retrieve(new RetrievalQuery(
                topic.getId(), List.of("캐시"), "캐시 재검증은 서버 확인이 필요한가?",
                reference, "모르겠습니다."
        ), 5);

        assertThat(result.chunks()).extracting(row -> row.chunk().getDocumentId())
                .containsExactly(evidence.getId());
        assertThat(result.conflictingEvidence()).isFalse();
    }

    private KnowledgeDocument publishAndChunk(Topic topic, Member admin, String title, String content) {
        KnowledgeDocument document = knowledgeDocumentRepository.save(KnowledgeDocument.builder()
                .topic(topic).createdByMember(admin).title(title)
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general").licenseNote("독립 작성").content(content).build());
        document.review(admin);
        document.publish();
        knowledgeDocumentRepository.save(document);
        knowledgeChunkService.generateChunks(document.getId());
        return document;
    }
}
