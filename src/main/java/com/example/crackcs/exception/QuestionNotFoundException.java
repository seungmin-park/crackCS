package com.example.crackcs.exception;

public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(Long questionId) {
        super("Question not found: " + questionId);
    }
}
