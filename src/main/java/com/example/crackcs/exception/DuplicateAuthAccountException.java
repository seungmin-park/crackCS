package com.example.crackcs.exception;

public class DuplicateAuthAccountException extends RuntimeException {

    public DuplicateAuthAccountException() {
        super("이미 가입된 이메일입니다.");
    }
}
