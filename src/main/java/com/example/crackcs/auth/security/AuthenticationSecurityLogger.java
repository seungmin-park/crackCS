package com.example.crackcs.auth.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationSecurityLogger {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationSecurityLogger.class);

    private final EmailMasker emailMasker;

    public void loginBlocked(String email) {
        log.warn("Repeated login attempts blocked for account={}", emailMasker.mask(email));
    }
}
