package com.example.crackcs.exception;

public class DuplicateContentCodeException extends RuntimeException {

    public DuplicateContentCodeException(String resource, String code) {
        super(resource + " code already exists: " + code);
    }
}
