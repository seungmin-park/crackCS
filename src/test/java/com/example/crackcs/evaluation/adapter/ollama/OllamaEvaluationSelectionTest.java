package com.example.crackcs.evaluation.adapter.ollama;

import com.example.crackcs.evaluation.adapter.StubEvaluationAdapter;
import com.example.crackcs.evaluation.port.EvaluationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaEvaluationSelectionTest {

    @Test
    @DisplayName("로컬에서 Ollama 평가를 켜면 평가 포트는 Ollama 어댑터 하나만 사용한다")
    void selectsOllamaWithoutStub() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withInitializer(context -> {
                    context.getEnvironment().setActiveProfiles("local");
                    context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
                })
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withUserConfiguration(StubEvaluationAdapter.class, OllamaEvaluationAdapter.class)
                .withPropertyValues("crackcs.evaluation.ollama.enabled=true");

        runner.run(context -> {
            assertThat(context).hasSingleBean(EvaluationPort.class);
            assertThat(context.getBean(EvaluationPort.class)).isInstanceOf(OllamaEvaluationAdapter.class);
        });
    }
}
