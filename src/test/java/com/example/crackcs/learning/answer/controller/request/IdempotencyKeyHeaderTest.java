package com.example.crackcs.learning.answer.controller.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyHeaderTest {
    private static final String CANONICAL_KEY = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    @DisplayName("소문자 canonical UUID를 멱등성 키로 보존한다")
    void preservesCanonicalLowercaseUuid() {
        IdempotencyKeyHeader idempotencyKeyHeader = IdempotencyKeyHeader.from(CANONICAL_KEY);

        assertThat(idempotencyKeyHeader.value()).isEqualTo(CANONICAL_KEY);
    }

    @Test
    @DisplayName("UUID 형식이 아닌 멱등성 키를 거부한다")
    void rejectsMalformedUuid() {
        assertThatThrownBy(() -> IdempotencyKeyHeader.from("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key는 소문자 UUID 형식이어야 합니다.");
    }

    @Test
    @DisplayName("대문자가 포함된 비정규 UUID를 거부한다")
    void rejectsUppercaseUuid() {
        assertThatThrownBy(() -> IdempotencyKeyHeader.from(CANONICAL_KEY.toUpperCase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key는 소문자 UUID 형식이어야 합니다.");
    }
}
