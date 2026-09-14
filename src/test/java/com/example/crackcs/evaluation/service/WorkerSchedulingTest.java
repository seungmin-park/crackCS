package com.example.crackcs.evaluation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerSchedulingTest {

    @Test
    @DisplayName("운영 스케줄러는 한 작업이 외부 응답을 기다려도 다른 작업을 실행한다")
    void runsAnotherTaskWhileOneTaskWaitsForProvider() throws IOException {
        List<PropertySource<?>> properties = new YamlPropertySourceLoader().load("production",
                new FileSystemResource("src/main/resources/application.yaml"));
        CountDownLatch providerStarted = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        CountDownLatch otherTaskCompleted = new CountDownLatch(1);

        new ApplicationContextRunner()
                .withInitializer(context -> properties.forEach(
                        property -> context.getEnvironment().getPropertySources().addLast(property)))
                .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
                .withUserConfiguration(SchedulingConfiguration.class)
                .run(context -> {
                    TaskScheduler scheduler = context.getBean(TaskScheduler.class);
                    try {
                        scheduler.schedule(() -> awaitProvider(providerStarted, releaseProvider), Instant.now());
                        assertThat(providerStarted.await(5, TimeUnit.SECONDS)).isTrue();

                        scheduler.schedule(otherTaskCompleted::countDown, Instant.now());

                        assertThat(otherTaskCompleted.await(2, TimeUnit.SECONDS))
                                .as("외부 응답 대기가 끝나기 전에 다른 스케줄 작업 실행")
                                .isTrue();
                    } finally {
                        releaseProvider.countDown();
                    }
                });
    }

    private void awaitProvider(CountDownLatch started, CountDownLatch release) {
        started.countDown();
        try {
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("provider wait was not released");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingConfiguration {
    }
}
