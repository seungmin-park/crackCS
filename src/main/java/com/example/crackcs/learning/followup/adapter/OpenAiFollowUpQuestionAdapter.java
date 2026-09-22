package com.example.crackcs.learning.followup.adapter;

import com.example.crackcs.evaluation.adapter.openai.OpenAiResponsesClient;
import com.example.crackcs.learning.followup.domain.FollowUpGenerationResult;
import com.example.crackcs.learning.followup.port.FollowUpQuestionGenerator;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "crackcs.followup.openai.enabled", havingValue = "true")
public class OpenAiFollowUpQuestionAdapter implements FollowUpQuestionGenerator {

    private final OpenAiResponsesClient openAiResponsesClient;
    private final OpenAiFollowUpRequestFactory openAiFollowUpRequestFactory;
    private final OpenAiFollowUpResponseParser openAiFollowUpResponseParser;
    private final Duration timeout;

    public OpenAiFollowUpQuestionAdapter(
            OpenAiResponsesClient openAiResponsesClient,
            OpenAiFollowUpRequestFactory openAiFollowUpRequestFactory,
            OpenAiFollowUpResponseParser openAiFollowUpResponseParser,
            @Value("${crackcs.followup.openai.timeout:30s}") Duration timeout
    ) {
        this.openAiResponsesClient = openAiResponsesClient;
        this.openAiFollowUpRequestFactory = openAiFollowUpRequestFactory;
        this.openAiFollowUpResponseParser = openAiFollowUpResponseParser;
        this.timeout = timeout;
    }

    @Override
    public FollowUpGenerationResult generate(FollowUpRequest followUpRequest) {
        long started = System.nanoTime();
        String requestBody = openAiFollowUpRequestFactory.create(followUpRequest);
        String responseBody = openAiResponsesClient.createResponse(requestBody, timeout);
        long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        return openAiFollowUpResponseParser.parse(responseBody, durationMillis);
    }
}
