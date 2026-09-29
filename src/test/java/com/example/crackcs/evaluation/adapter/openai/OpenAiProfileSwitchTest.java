package com.example.crackcs.evaluation.adapter.openai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiProfileSwitchTest {

    @Test
    @DisplayName("PostgreSQL 설정에서 평가만 켜도 후속 질문은 꺼진 상태를 유지한다")
    void enablesEvaluationWithoutFollowUp() throws IOException {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("environment", Map.of(
                "OPENAI_EVALUATION_ENABLED", "true",
                "OPENAI_FOLLOWUP_ENABLED", "false"
        )));
        propertySources.addLast(new YamlPropertySourceLoader()
                .load("postgres", new ClassPathResource("application-postgres.yaml")).getFirst());
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);

        assertThat(resolver.getProperty("crackcs.evaluation.openai.enabled")).isEqualTo("true");
        assertThat(resolver.getProperty("crackcs.followup.openai.enabled")).isEqualTo("false");
    }

    @Test
    @DisplayName("로컬 PostgreSQL에서도 키와 평가 스위치만 설정하면 유료 평가를 켤 수 있다")
    void enablesEvaluationInLocalPostgresProfile() throws IOException {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("environment", Map.of(
                "OPENAI_EVALUATION_ENABLED", "true",
                "OPENAI_API_KEY", "test-secret"
        )));
        propertySources.addLast(new YamlPropertySourceLoader()
                .load("local-postgres", new ClassPathResource("application-local-postgres.yaml")).getFirst());
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);

        assertThat(resolver.getProperty("crackcs.evaluation.openai.enabled")).isEqualTo("true");
        assertThat(resolver.getProperty("crackcs.evaluation.openai.api-key")).isEqualTo("test-secret");
        assertThat(resolver.getProperty("crackcs.followup.openai.enabled")).isEqualTo("false");
    }
}
