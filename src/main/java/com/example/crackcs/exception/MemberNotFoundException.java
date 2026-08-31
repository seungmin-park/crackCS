package com.example.crackcs.exception;

public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(Long memberId) {
        super("Member not found: " + memberId);
    }
}
