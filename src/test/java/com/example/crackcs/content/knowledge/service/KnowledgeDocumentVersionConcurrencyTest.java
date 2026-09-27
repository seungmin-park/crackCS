package com.example.crackcs.content.knowledge.service;

import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocumentStatus;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import com.example.crackcs.support.ConcurrentRequests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class KnowledgeDocumentVersionConcurrencyTest {
    @Autowired private KnowledgeDocumentService knowledgeDocumentService;
    @Autowired private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private MemberRepository memberRepository;

    @Autowired private TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        knowledgeDocumentRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("검수 트랜잭션이 끝나기 전에 공개가 앞질러 커밋되지 않아 오래된 초안이 공개 상태를 덮어쓰지 않는다")
    void publicationWaitsForInFlightReview() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("문서 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        KnowledgeDocument draft = knowledgeDocumentService.create(admin.getId(), new KnowledgeDocumentDraft(
                topic.getId(), "원본 문서", KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "원본 근거"));
        knowledgeDocumentService.review(draft.getId(), admin.getId());

        CountDownLatch reviewed = new CountDownLatch(1);
        CountDownLatch releaseReview = new CountDownLatch(1);
        CountDownLatch publishing = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> review = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                knowledgeDocumentService.review(draft.getId(), admin.getId());
                reviewed.countDown();
                try {
                    if (!releaseReview.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Review release timed out");
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                }
            }));
            assertThat(reviewed.await(10, TimeUnit.SECONDS)).isTrue();
            Future<KnowledgeDocument> publication = executor.submit(() -> {
                publishing.countDown();
                return knowledgeDocumentService.publishAsCurrentVersion(draft.getId());
            });
            assertThat(publishing.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> publication.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseReview.countDown();
            review.get(10, TimeUnit.SECONDS);
            publication.get(10, TimeUnit.SECONDS);

            assertThat(knowledgeDocumentService.findById(draft.getId()).getStatus()).isEqualTo(KnowledgeDocumentStatus.PUBLISHED);
        } finally {
            releaseReview.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("같은 공개 문서에서 동시에 만든 여덟 초안은 모두 다른 버전으로 저장된다")
    void allocatesDistinctVersionsForConcurrentDrafts() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("문서 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        KnowledgeDocument original = knowledgeDocumentService.create(admin.getId(), new KnowledgeDocumentDraft(
                topic.getId(), "원본 문서", KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "원본 근거"));
        knowledgeDocumentService.review(original.getId(), admin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(original.getId());

        List<Callable<KnowledgeDocument>> requests = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            KnowledgeDocumentDraft draft = new KnowledgeDocumentDraft(topic.getId(), "후속 문서 " + index,
                    KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "후속 근거 " + index);
            requests.add(() -> knowledgeDocumentService.createNextVersion(original.getId(), admin.getId(), draft));
        }
        ConcurrentRequests.run(requests);

        assertThat(knowledgeDocumentRepository.findAll()).extracting(KnowledgeDocument::getDocumentVersion)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(knowledgeDocumentRepository.findById(original.getId()).orElseThrow().getStatus()).isEqualTo(KnowledgeDocumentStatus.PUBLISHED);
    }

    @Test
    @DisplayName("최초 문서가 폐기된 뒤 현재 공개본에서도 동시 버전 번호가 겹치지 않는다")
    void allocatesVersionsFromCurrentVersionAfterOriginalRetirement() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("문서 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        KnowledgeDocument original = knowledgeDocumentService.create(admin.getId(), new KnowledgeDocumentDraft(
                topic.getId(), "원본 문서", KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "원본 근거"));
        knowledgeDocumentService.review(original.getId(), admin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(original.getId());

        KnowledgeDocument current = knowledgeDocumentService.createNextVersion(original.getId(), admin.getId(),
                new KnowledgeDocumentDraft(topic.getId(), "현재 문서", KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "현재 근거"));
        knowledgeDocumentService.review(current.getId(), admin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(current.getId());

        List<Callable<KnowledgeDocument>> requests = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            KnowledgeDocumentDraft draft = new KnowledgeDocumentDraft(topic.getId(), "후속 문서 " + index,
                    KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "후속 근거 " + index);
            requests.add(() -> knowledgeDocumentService.createNextVersion(current.getId(), admin.getId(), draft));
        }
        ConcurrentRequests.run(requests);

        assertThat(knowledgeDocumentRepository.findAll()).extracting(KnowledgeDocument::getDocumentVersion)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(knowledgeDocumentRepository.findById(original.getId()).orElseThrow().getStatus()).isEqualTo(KnowledgeDocumentStatus.RETIRED);
    }

    @Test
    @DisplayName("같은 계열의 두 검수본을 동시에 공개해도 공개 문서는 하나만 남는다")
    void keepsOnePublishedVersionAfterConcurrentPublication() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("문서 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        KnowledgeDocument original = knowledgeDocumentService.create(admin.getId(), new KnowledgeDocumentDraft(
                topic.getId(), "원본 문서", KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "원본 근거"));
        knowledgeDocumentService.review(original.getId(), admin.getId());
        knowledgeDocumentService.publishAsCurrentVersion(original.getId());
        KnowledgeDocument second = knowledgeDocumentService.createNextVersion(original.getId(), admin.getId(),
                new KnowledgeDocumentDraft(topic.getId(), "두 번째 문서", KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "두 번째 근거"));
        KnowledgeDocument third = knowledgeDocumentService.createNextVersion(original.getId(), admin.getId(),
                new KnowledgeDocumentDraft(topic.getId(), "세 번째 문서", KnowledgeSourceType.INTERNAL_SUMMARY, null, "Java 21", "직접 작성", "세 번째 근거"));
        knowledgeDocumentService.review(second.getId(), admin.getId());
        knowledgeDocumentService.review(third.getId(), admin.getId());

        ConcurrentRequests.run(List.of(
                () -> knowledgeDocumentService.publishAsCurrentVersion(second.getId()),
                () -> knowledgeDocumentService.publishAsCurrentVersion(third.getId())
        ));

        assertThat(knowledgeDocumentRepository.findAllByVersionSeriesIdAndStatus(original.getVersionSeriesId(), KnowledgeDocumentStatus.PUBLISHED)).hasSize(1);
        assertThat(knowledgeDocumentRepository.findAllByVersionSeriesIdAndStatus(original.getVersionSeriesId(), KnowledgeDocumentStatus.RETIRED)).hasSize(2);
        assertThat(knowledgeDocumentRepository.findById(original.getId()).orElseThrow().getContent()).isEqualTo("원본 근거");
    }
}
