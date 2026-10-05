package com.example.crackcs.auth.service;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

final class AuthenticationThrottleConflicts {
    private AuthenticationThrottleConflicts() {}

    static boolean isUniqueKeyConflict(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraint) {
                String name = constraint.getConstraintName();
                return "23505".equals(constraint.getSQLException().getSQLState()) && name != null
                        && (name.toLowerCase(Locale.ROOT).contains("uk_login_attempt_key")
                        || name.toLowerCase(Locale.ROOT).contains("uk_auth_request_bucket_key"));
            }
        }
        return false;
    }
}
