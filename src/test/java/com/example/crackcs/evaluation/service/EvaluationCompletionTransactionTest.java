package com.example.crackcs.evaluation.service;

import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class EvaluationCompletionTransactionTest {
    @Autowired
    EvaluationCompletionTransaction completionTransaction;
    @Autowired
    TopicRepository topics;
    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        topics.deleteAllInBatch();
    }

    @ParameterizedTest
    @ValueSource(strings = {"update topic set name = null where id = ?",
            "update topic set parent_id = -1 where id = ?"})
    @DisplayName("영구 무결성 오류는 한 번만 시도하고 같은 트랜잭션의 저장을 롤백한다")
    void doesNotRetryPermanentIntegrityFailures(String invalidUpdate) {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> completionTransaction.execute(() -> {
            attempts.incrementAndGet();
            Topic topic = topics.save(Topic.builder().code("ROLLBACK").name("롤백 확인").build());
            jdbc.update(invalidUpdate, topic.getId());
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(attempts.get()).isEqualTo(1);
        assertThat(topics.count()).isZero();
    }

    @Test
    @DisplayName("학습 상태와 무관한 유일키 오류는 재시도 없이 부분 저장을 롤백한다")
    void doesNotRetryUnrelatedUniqueConstraint() {
        topics.save(Topic.builder().code("DUPLICATE").name("기존 주제").build());
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> completionTransaction.execute(() -> {
            attempts.incrementAndGet();
            topics.save(Topic.builder().code("ROLLBACK").name("부분 저장").build());
            topics.save(Topic.builder().code("DUPLICATE").name("중복 주제").build());
        })).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(attempts.get()).isEqualTo(1);
        assertThat(topics.findAll()).extracting(Topic::getCode).containsExactly("DUPLICATE");
    }
}
