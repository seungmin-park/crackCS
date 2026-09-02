package com.example.crackcs.exception;

public class KnowledgeDocumentNotFoundException extends RuntimeException {

    public KnowledgeDocumentNotFoundException(Long documentId) {
        super("KnowledgeDocument not found: " + documentId);
    }
}
