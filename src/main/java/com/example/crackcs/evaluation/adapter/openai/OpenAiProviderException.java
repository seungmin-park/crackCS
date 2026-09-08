package com.example.crackcs.evaluation.adapter.openai;

public class OpenAiProviderException extends RuntimeException {
    public OpenAiProviderException(String message) {
        super(message);
    }

    public OpenAiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
