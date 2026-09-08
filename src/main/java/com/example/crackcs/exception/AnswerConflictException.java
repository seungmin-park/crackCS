package com.example.crackcs.exception;

public class AnswerConflictException extends RuntimeException {
    public AnswerConflictException() {
        super("같은 요청 식별자로 다른 답변을 제출할 수 없습니다.");
    }
}
