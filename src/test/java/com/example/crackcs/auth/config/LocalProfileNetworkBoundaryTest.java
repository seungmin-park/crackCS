package com.example.crackcs.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalProfileNetworkBoundaryTest {

    @Test
    @DisplayName("local 프로필은 API와 관리 포트를 loopback에만 연다")
    void bindsLocalListenersToLoopback() throws IOException {
        PropertySource<?> localProfile = loadLocalProfile();

        assertThat(localProfile.getProperty("server.address")).hasToString("127.0.0.1");
        assertThat(localProfile.getProperty("management.server.address")).hasToString("127.0.0.1");
    }

    @Test
    @DisplayName("local 프로필은 loopback 호스트 이름만 허용한다")
    void allowsOnlyLoopbackHostNames() throws IOException {
        PropertySource<?> localProfile = loadLocalProfile();

        assertThat(localProfile.getProperty("crackcs.security.allowed-hosts")).hasToString("localhost,127.0.0.1,[::1]");
    }

    private PropertySource<?> loadLocalProfile() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application-local", new ClassPathResource("application-local.yaml"));
        return sources.getFirst();
    }
}
