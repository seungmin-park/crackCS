package com.example.crackcs.evaluation.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationOperationLoggerTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(EvaluationOperationLogger.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("평가 운영 로그는 상관 ID와 도메인 결과만 기록한다")
    void logsCorrelationAndOutcomesWithoutSensitiveContent() {
        appender.start();
        logger.addAppender(appender);
        EvaluationOperationLogger operationLogger = new EvaluationOperationLogger();

        operationLogger.retrievalCompleted(3L, 5L, 7L, 2, List.of(11L, 13L));
        operationLogger.evaluationCompleted(3L, 5L, 7L, "test-model", "rule-v1");
        operationLogger.evaluationFailed(3L, 5L, 7L, "PROVIDER_TIMEOUT");

        String messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertThat(messages).contains(
                "evaluationId=3", "answerId=5", "memberId=7",
                "candidateCount=2", "evidenceIds=[11, 13]",
                "model=test-model", "evaluatorVersion=rule-v1", "failureCode=PROVIDER_TIMEOUT"
        );
        assertThat(messages).doesNotContain("latencyMillis",
                "answer-secret-sentinel",
                "password-secret-sentinel",
                "api-key-secret-sentinel",
                "evidence-content-secret-sentinel"
        );
    }
}
