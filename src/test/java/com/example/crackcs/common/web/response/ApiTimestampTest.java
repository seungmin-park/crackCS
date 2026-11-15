package com.example.crackcs.common.web.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ApiTimestampTest {

    private static final LocalDateTime SERVER_LOCAL_TIME = LocalDateTime.of(2026, 11, 15, 16, 42, 40);

    @Test
    @DisplayName("한국 시간대 서버의 로컬 시각은 9시간 앞선 UTC 시각이 된다")
    void convertsSeoulLocalTimeToUtc() {
        assertThat(ApiTimestamp.from(SERVER_LOCAL_TIME, ZoneId.of("Asia/Seoul")))
                .isEqualTo(Instant.parse("2026-11-15T07:42:40Z"));
    }

    @Test
    @DisplayName("UTC 서버의 로컬 시각은 같은 숫자의 UTC 시각이 된다")
    void keepsUtcLocalTime() {
        assertThat(ApiTimestamp.from(SERVER_LOCAL_TIME, ZoneOffset.UTC))
                .isEqualTo(Instant.parse("2026-11-15T16:42:40Z"));
    }

    @Test
    @DisplayName("서버 시간대를 따로 주지 않으면 JVM 기본 시간대로 바꾼다")
    void usesSystemDefaultZone() {
        assertThat(ApiTimestamp.from(SERVER_LOCAL_TIME))
                .isEqualTo(SERVER_LOCAL_TIME.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("시각이 없으면 null을 돌려준다")
    void keepsMissingTime() {
        assertThat(ApiTimestamp.from(null)).isNull();
    }
}
