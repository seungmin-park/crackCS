package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.exception.ProviderRequestRejectedException;

import java.nio.charset.StandardCharsets;

public final class OpenAiOutputTextLimit {
    private static final int MAX_OUTPUT_TEXT_BYTES = 65_536;

    private OpenAiOutputTextLimit() {}

    public static void requireWithinLimit(String outputText) {
        // The length check avoids making another byte array for an already oversized string.
        if (outputText != null && (outputText.length() > MAX_OUTPUT_TEXT_BYTES
                || outputText.getBytes(StandardCharsets.UTF_8).length > MAX_OUTPUT_TEXT_BYTES)) {
            throw new ProviderRequestRejectedException("PROVIDER_RESPONSE_TOO_LARGE");
        }
    }
}
