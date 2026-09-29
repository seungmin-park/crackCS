package com.example.crackcs.exception;

public class ProviderRequestRejectedException extends RuntimeException {

    private final String reason;

    public ProviderRequestRejectedException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
