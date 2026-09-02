package com.example.crackcs.exception;

public class DuplicateKnowledgeDocumentException extends RuntimeException {

    public DuplicateKnowledgeDocumentException() {
        super("동일한 원문의 KnowledgeDocument가 이미 존재합니다.");
    }
}
