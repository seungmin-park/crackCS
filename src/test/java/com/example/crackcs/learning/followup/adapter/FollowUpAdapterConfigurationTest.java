package com.example.crackcs.learning.followup.adapter;

import com.example.crackcs.evaluation.adapter.openai.JdkOpenAiResponsesClient;
import com.example.crackcs.evaluation.adapter.openai.OpenAiResponsesClient;
import com.example.crackcs.learning.followup.port.FollowUpQuestionGenerator;
import com.example.crackcs.learning.followup.repository.FollowUpGenerationRepository;
import com.example.crackcs.learning.followup.service.FollowUpQuestionProcessor;
import com.example.crackcs.learning.followup.service.FollowUpWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FollowUpAdapterConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(JdkOpenAiResponsesClient.class, OpenAiFollowUpQuestionAdapter.class,
                    StubFollowUpQuestionAdapter.class).withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("Worker가 활성화됐는데 생성기가 없으면 시작 단계에서 거부한다")
    void enabledWorkerRequiresGenerator() {
        workerContext().run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("Worker를 명시적으로 끄면 생성기 없이 시작할 수 있다")
    void disabledWorkerDoesNotRequireGenerator() {
        workerContext().withPropertyValues("crackcs.followup.worker-enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("생성기를 설정하면 Worker를 시작할 수 있다")
    void configuredGeneratorStartsWorker() {
        workerContext().withBean(FollowUpQuestionGenerator.class, StubFollowUpQuestionAdapter::new)
                .run(context -> assertThat(context).hasNotFailed());
    }

    private ApplicationContextRunner workerContext() {
        return new ApplicationContextRunner().withUserConfiguration(FollowUpWorker.class)
                .withBean(FollowUpGenerationRepository.class, () -> mock(FollowUpGenerationRepository.class))
                .withBean(FollowUpQuestionProcessor.class, () -> answerId -> {
                });
    }

    @Test
    @DisplayName("후속 생성만 명시적으로 켜도 기존 키 설정으로 실제 client를 구성한다")
    void followUpOptInConfiguresSharedClient() {
        runner.withPropertyValues("crackcs.followup.openai.enabled=true", "crackcs.evaluation.openai.enabled=false",
                        "crackcs.evaluation.openai.api-key=test-only-never-used")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OpenAiResponsesClient.class);
                    assertThat(context).hasSingleBean(FollowUpQuestionGenerator.class);
                    assertThat(context).doesNotHaveBean(StubFollowUpQuestionAdapter.class);
                });
    }

    @Test
    @DisplayName("운영 환경은 기본 설정에서 stub으로 대체하지 않는다")
    void productionHasNoFallback() {
        runner.withPropertyValues("spring.profiles.active=production")
                .run(context -> assertThat(context).doesNotHaveBean(FollowUpQuestionGenerator.class));
    }

    @Test
    @DisplayName("local과 production을 함께 지정해도 stub을 사용하지 않는다")
    void mixedProfilesHaveNoFallback() {
        runner.withPropertyValues("spring.profiles.active=local,production")
                .run(context -> assertThat(context).doesNotHaveBean(FollowUpQuestionGenerator.class));
    }
}
