package com.example.crackcs.exception;

public class TooManyAnswerRequestsException extends RuntimeException {
    public TooManyAnswerRequestsException() {
        super("답변 요청이 너무 많습니다. 1분 뒤 다시 시도해 주세요.");
    }
}
