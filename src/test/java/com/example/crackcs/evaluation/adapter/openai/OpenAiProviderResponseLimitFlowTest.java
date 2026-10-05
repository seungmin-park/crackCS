package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.port.EvaluationConceptInput;
import com.example.crackcs.evaluation.port.EvaluationEvidenceInput;
import com.example.crackcs.evaluation.port.EvaluationPort;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.exception.ProviderRequestRejectedException;
import com.example.crackcs.learning.followup.adapter.OpenAiFollowUpQuestionAdapter;
import com.example.crackcs.learning.followup.adapter.OpenAiFollowUpRequestFactory;
import com.example.crackcs.learning.followup.adapter.OpenAiFollowUpResponseParser;
import com.example.crackcs.learning.followup.port.FollowUpQuestionGenerator;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.convert.ApplicationConversionService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class OpenAiProviderResponseLimitFlowTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    @DisplayName("평가 adapter는 Spring에서 설정한 공용 HTTP byte 제한을 적용한다")
    void boundsEvaluationProviderResponse() throws IOException {
        ApplicationContextRunner contextRunner = contextWithOversizedProvider();
        EvaluationRequest request = new EvaluationRequest(1L, "원본 질문", "기준 답안", "회원 답안",
                List.of(new EvaluationConceptInput(11L, "개념", true)),
                List.of(new EvaluationEvidenceInput(7L, 2L, "근거", 1, 0, 2, "근거", 5.0)));

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThatThrownBy(() -> context.getBean(EvaluationPort.class).evaluate(request))
                    .isInstanceOf(ProviderRequestRejectedException.class).hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
        });
    }

    @Test
    @DisplayName("후속 질문만 켠 adapter도 같은 공용 HTTP byte 제한을 적용한다")
    void boundsFollowUpProviderResponseWithoutEvaluationEnabled() throws IOException {
        ApplicationContextRunner contextRunner = contextWithOversizedProvider()
                .withPropertyValues("crackcs.evaluation.openai.enabled=false");
        FollowUpRequest request = new FollowUpRequest("원본 질문", 11L, "개념", Verdict.CORRECT, "피드백",
                List.of(), List.of(), List.of(new FollowUpRequest.Evidence(7L, "근거")));

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThatThrownBy(() -> context.getBean(FollowUpQuestionGenerator.class).generate(request))
                    .isInstanceOf(ProviderRequestRejectedException.class).hasMessage("PROVIDER_RESPONSE_TOO_LARGE");
        });
    }

    private ApplicationContextRunner contextWithOversizedProvider() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/responses", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(new byte[65]);
            }
        });
        server.start();
        return new ApplicationContextRunner().withBean(ObjectMapper.class, ObjectMapper::new)
                .withInitializer(context -> context.getBeanFactory()
                        .setConversionService(ApplicationConversionService.getSharedInstance()))
                .withUserConfiguration(JdkOpenAiResponsesClient.class, OpenAiEvaluationAdapter.class,
                        OpenAiEvaluationRequestFactory.class, OpenAiEvaluationResponseParser.class,
                        OpenAiFollowUpQuestionAdapter.class, OpenAiFollowUpRequestFactory.class,
                        OpenAiFollowUpResponseParser.class)
                .withPropertyValues("crackcs.evaluation.openai.enabled=true", "crackcs.followup.openai.enabled=true",
                        "crackcs.evaluation.openai.api-key=test-only", "crackcs.evaluation.openai.max-response-bytes=64",
                        "crackcs.evaluation.openai.endpoint=http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses");
    }
}
