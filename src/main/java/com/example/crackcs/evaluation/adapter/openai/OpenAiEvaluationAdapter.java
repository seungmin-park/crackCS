package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "crackcs.evaluation.openai.enabled", havingValue = "true")
public class OpenAiEvaluationAdapter implements EvaluationPort {

    private final OpenAiResponsesClient openAiResponsesClient;
    private final OpenAiEvaluationRequestFactory openAiEvaluationRequestFactory;
    private final OpenAiEvaluationResponseParser openAiEvaluationResponseParser;
    private final Duration timeout;

    public OpenAiEvaluationAdapter(
            OpenAiResponsesClient openAiResponsesClient,
            OpenAiEvaluationRequestFactory openAiEvaluationRequestFactory,
            OpenAiEvaluationResponseParser openAiEvaluationResponseParser,
            @Value("${crackcs.evaluation.openai.timeout:30s}") Duration timeout
    ) {
        this.openAiResponsesClient = openAiResponsesClient;
        this.openAiEvaluationRequestFactory = openAiEvaluationRequestFactory;
        this.openAiEvaluationResponseParser = openAiEvaluationResponseParser;
        this.timeout = timeout;
    }

    @Override
    public EvaluationResult evaluate(EvaluationRequest evaluationRequest) {
        long started = System.nanoTime();
        String requestBody = openAiEvaluationRequestFactory.create(evaluationRequest);
        String responseBody = openAiResponsesClient.createResponse(requestBody, timeout);
        long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        return openAiEvaluationResponseParser.parse(responseBody, durationMillis);
    }
}
