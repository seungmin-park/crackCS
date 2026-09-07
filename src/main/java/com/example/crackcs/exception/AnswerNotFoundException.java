package com.example.crackcs.exception;

public class AnswerNotFoundException extends RuntimeException {
    public AnswerNotFoundException(Long id) { super("답변을 찾을 수 없습니다."); }
}
