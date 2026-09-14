package com.example.crackcs.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailMaskerTest {

    private final EmailMasker emailMasker = new EmailMasker();

    @Test
    @DisplayName("인증 로그용 이메일은 첫 글자와 domain만 남기고 마스킹한다")
    void masksEmailForSecurityLog() {
        assertThat(emailMasker.mask("user@example.com")).isEqualTo("u***@example.com");
    }
}
