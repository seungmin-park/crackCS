package com.example.crackcs.content.question.service;

public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(Long questionId) {
        super("Question not found: " + questionId);
    }
}
