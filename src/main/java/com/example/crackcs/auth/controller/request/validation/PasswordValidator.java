package com.example.crackcs.auth.controller.request.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_CODE_POINTS = 15;
    private static final int MAX_CODE_POINTS = 64;
    private static final int MAX_UTF8_BYTES = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        int codePoints = password.codePointCount(0, password.length());
        if (codePoints < MIN_CODE_POINTS || codePoints > MAX_CODE_POINTS) {
            return false;
        }
        if (password.codePoints().anyMatch(Character::isISOControl)) {
            return false;
        }
        return password.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES;
    }
}
