package com.example.crackcs.exception;

public class EvaluationNotFoundException extends RuntimeException {
    public EvaluationNotFoundException(Long evaluationId) {
        super("평가를 찾을 수 없습니다.");
    }
}
