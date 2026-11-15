package com.example.crackcs.common.web.response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 응답에 싣는 시각. 저장된 값은 서버 시간대의 로컬 시각이라 그대로 내보내면
 * 시간대가 다른 화면에서 다른 시각으로 읽힌다. 시간대가 드러나는 UTC 시각으로 바꿔 내보낸다.
 */
public final class ApiTimestamp {

    private ApiTimestamp() {
    }

    public static Instant from(LocalDateTime serverLocalTime) {
        return from(serverLocalTime, ZoneId.systemDefault());
    }

    static Instant from(LocalDateTime serverLocalTime, ZoneId serverZone) {
        return serverLocalTime == null ? null : serverLocalTime.atZone(serverZone).toInstant();
    }
}
