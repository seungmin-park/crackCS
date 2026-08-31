package com.example.crackcs.auth.security;

import org.springframework.stereotype.Component;

@Component
public class EmailMasker {

    public String mask(String email) {
        if (email == null) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
