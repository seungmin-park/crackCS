package com.example.crackcs.exception;

public class ConceptNotFoundException extends RuntimeException {

    public ConceptNotFoundException(Long conceptId) {
        super("Concept not found: " + conceptId);
    }
}
