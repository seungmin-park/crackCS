package com.example.crackcs.integration.postgres;

import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("postgres")
@SpringBootTest
class PostgresPersistenceTest {
    @Autowired DataSource dataSource;
    @Autowired TopicRepository topics;

    @AfterEach
    void tearDown() {
        topics.deleteAllInBatch();
    }

    @Test
    @DisplayName("PostgreSQL 통합 검증은 H2 대체 없이 PostgreSQL 17에서 실행한다")
    void requiresRealPostgres17() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(17);
        }
    }

    @Test
    @DisplayName("PostgreSQL에 생성한 Topic 스키마가 중복 업무 코드를 거부한다")
    void enforcesMappedUniqueConstraintAfterCommit() {
        Topic saved = topics.save(Topic.builder().code("PG_UNIQUE").name("첫 번째 토픽").build());

        assertThatThrownBy(() -> topics.save(Topic.builder().code("PG_UNIQUE").name("중복 토픽").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(topics.findById(saved.getId()).orElseThrow().getName()).isEqualTo("첫 번째 토픽");
        assertThat(topics.count()).isEqualTo(1);
    }
}
