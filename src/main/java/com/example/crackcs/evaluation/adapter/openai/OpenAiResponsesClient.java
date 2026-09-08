package com.example.crackcs.evaluation.adapter.openai;

import java.time.Duration;

public interface OpenAiResponsesClient {
    String createResponse(String requestBody, Duration timeout);
}
