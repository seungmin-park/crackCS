package com.example.crackcs.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationSecurityLogger {

    private final EmailMasker emailMasker;

    public void loginBlocked(String email) {
        log.warn("Repeated login attempts blocked for account={}", emailMasker.mask(email));
    }
}
