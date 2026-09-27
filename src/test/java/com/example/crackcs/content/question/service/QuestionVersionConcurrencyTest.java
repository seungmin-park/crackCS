package com.example.crackcs.content.question.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.repository.QuestionConceptRepository;
import com.example.crackcs.content.question.repository.QuestionRepository;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
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
class QuestionVersionConcurrencyTest {
    @Autowired private QuestionService questionService;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionConceptRepository questionConceptRepository;
    @Autowired private ConceptRepository conceptRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private MemberRepository memberRepository;

    @Autowired private TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        questionConceptRepository.deleteAllInBatch();
        questionRepository.deleteAllInBatch();
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("검수 트랜잭션이 끝나기 전에 공개가 앞질러 커밋되지 않아 오래된 초안이 공개 상태를 덮어쓰지 않는다")
    void publicationWaitsForInFlightReview() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("버전 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("PROCESS").name("프로세스").build());
        Question draft = questionService.create(admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "원본 질문", "원본 정답");
        questionService.replaceConcepts(draft.getId(), List.of(new QuestionConceptCriterion(concept.getId(), BigDecimal.ONE, true)));
        questionService.review(draft.getId(), admin.getId());

        CountDownLatch reviewed = new CountDownLatch(1);
        CountDownLatch releaseReview = new CountDownLatch(1);
        CountDownLatch publishing = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> review = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                questionService.review(draft.getId(), admin.getId());
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
            Future<Question> publication = executor.submit(() -> {
                publishing.countDown();
                return questionService.publishAsCurrentVersion(draft.getId());
            });
            assertThat(publishing.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> publication.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseReview.countDown();
            review.get(10, TimeUnit.SECONDS);
            publication.get(10, TimeUnit.SECONDS);

            assertThat(questionService.findById(draft.getId()).getStatus()).isEqualTo(QuestionStatus.PUBLISHED);
        } finally {
            releaseReview.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("같은 공개 문제에서 동시에 만든 여덟 초안의 버전 번호가 겹치지 않는다")
    void allocatesDistinctVersionsForConcurrentDrafts() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("버전 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("PROCESS").name("프로세스").build());
        Question original = questionService.create(admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "원본 질문", "원본 정답");
        questionService.replaceConcepts(original.getId(), List.of(new QuestionConceptCriterion(concept.getId(), BigDecimal.ONE, true)));
        questionService.review(original.getId(), admin.getId());
        questionService.publishAsCurrentVersion(original.getId());

        List<Callable<Question>> requests = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            String content = "동시 질문 " + index;
            requests.add(() -> questionService.createNextVersion(original.getId(), admin.getId(), QuestionDifficulty.BASIC, content, "정답"));
        }
        ConcurrentRequests.run(requests);

        assertThat(questionRepository.findAll()).extracting(Question::getQuestionVersion)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(questionRepository.findById(original.getId()).orElseThrow().getStatus()).isEqualTo(QuestionStatus.PUBLISHED);
    }

    @Test
    @DisplayName("최초 문제가 폐기된 뒤 현재 공개본에서도 동시 버전 번호가 겹치지 않는다")
    void allocatesVersionsFromCurrentVersionAfterOriginalRetirement() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("버전 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("PROCESS").name("프로세스").build());
        Question original = questionService.create(admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "원본 질문", "원본 정답");
        questionService.replaceConcepts(original.getId(), List.of(new QuestionConceptCriterion(concept.getId(), BigDecimal.ONE, true)));
        questionService.review(original.getId(), admin.getId());
        questionService.publishAsCurrentVersion(original.getId());

        Question current = questionService.createNextVersion(original.getId(), admin.getId(), QuestionDifficulty.BASIC, "현재 질문", "현재 정답");
        questionService.review(current.getId(), admin.getId());
        questionService.publishAsCurrentVersion(current.getId());

        List<Callable<Question>> requests = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            String content = "동시 질문 " + index;
            requests.add(() -> questionService.createNextVersion(current.getId(), admin.getId(), QuestionDifficulty.BASIC, content, "정답"));
        }
        ConcurrentRequests.run(requests);

        assertThat(questionRepository.findAll()).extracting(Question::getQuestionVersion)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(questionRepository.findById(original.getId()).orElseThrow().getStatus()).isEqualTo(QuestionStatus.RETIRED);
    }

    @Test
    @DisplayName("같은 계열의 두 검수본을 동시에 공개해도 공개 문제는 하나만 남는다")
    void keepsOnePublishedVersionAfterConcurrentPublication() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("버전 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("PROCESS").name("프로세스").build());
        Question original = questionService.create(admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "원본 질문", "원본 정답");
        questionService.replaceConcepts(original.getId(), List.of(new QuestionConceptCriterion(concept.getId(), BigDecimal.ONE, true)));
        questionService.review(original.getId(), admin.getId());
        questionService.publishAsCurrentVersion(original.getId());
        Question second = questionService.createNextVersion(original.getId(), admin.getId(), QuestionDifficulty.BASIC, "두 번째 질문", "정답");
        Question third = questionService.createNextVersion(original.getId(), admin.getId(), QuestionDifficulty.BASIC, "세 번째 질문", "정답");
        questionService.review(second.getId(), admin.getId());
        questionService.review(third.getId(), admin.getId());

        ConcurrentRequests.run(List.of(
                () -> questionService.publishAsCurrentVersion(second.getId()),
                () -> questionService.publishAsCurrentVersion(third.getId())
        ));

        assertThat(questionRepository.findAllByVersionSeriesIdAndStatus(original.getVersionSeriesId(), QuestionStatus.PUBLISHED)).hasSize(1);
        assertThat(questionRepository.findAllByVersionSeriesIdAndStatus(original.getVersionSeriesId(), QuestionStatus.RETIRED)).hasSize(2);
        assertThat(questionRepository.findById(original.getId()).orElseThrow().getContent()).isEqualTo("원본 질문");
    }

    @Test
    @DisplayName("서비스를 우회해도 같은 문제 계열의 같은 버전은 중복 저장할 수 없다")
    void rejectsDuplicateVersionAtDatabaseBoundary() {
        Member admin = memberRepository.save(Member.builder().nickname("버전 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("VERSION_TEST").name("운영체제").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("PROCESS").name("프로세스").build());
        Question original = questionService.create(admin.getId(), topic.getId(), QuestionDifficulty.BASIC, "원본 질문", "원본 정답");
        questionService.replaceConcepts(original.getId(), List.of(new QuestionConceptCriterion(concept.getId(), BigDecimal.ONE, true)));
        questionService.review(original.getId(), admin.getId());
        questionService.publishAsCurrentVersion(original.getId());
        Question source = questionService.findById(original.getId());
        Question firstDraft = source.createNextVersion(2, admin, QuestionDifficulty.BASIC, "두 번째 질문", "정답");
        questionRepository.save(firstDraft);
        Question duplicateDraft = source.createNextVersion(2, admin, QuestionDifficulty.BASIC, "중복 버전 질문", "정답");

        assertThatThrownBy(() -> questionRepository.save(duplicateDraft)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(questionRepository.findAll()).extracting(Question::getQuestionVersion).containsExactlyInAnyOrder(1, 2);
    }
}
