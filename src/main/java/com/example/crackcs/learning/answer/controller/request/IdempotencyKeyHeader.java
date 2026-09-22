package com.example.crackcs.learning.answer.controller.request;

import java.util.UUID;

public record IdempotencyKeyHeader(String value) {
    private static final String INVALID_FORMAT_MESSAGE =
            "Idempotency-Key는 소문자 UUID 형식이어야 합니다.";

    public IdempotencyKeyHeader {
        if (!isCanonicalLowercaseUuid(value)) {
            throw new IllegalArgumentException(INVALID_FORMAT_MESSAGE);
        }
    }

    public static IdempotencyKeyHeader from(String value) {
        return new IdempotencyKeyHeader(value);
    }

    private static boolean isCanonicalLowercaseUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException invalidUuid) {
            return false;
        }
    }
}
