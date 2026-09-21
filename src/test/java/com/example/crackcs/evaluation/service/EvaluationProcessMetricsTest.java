package com.example.crackcs.evaluation.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "crackcs.evaluation.worker-enabled=false")
class EvaluationProcessMetricsTest {

    @Autowired
    private EvaluationProcessor evaluationProcessor;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("평가 처리 public 경계를 호출하면 처리 시간 메트릭을 기록한다")
    void recordsEvaluationProcessingTimeAtPublicBoundary() {
        Timer timerBeforeCall = meterRegistry.find("crackcs.evaluation.process").timer();
        long countBeforeCall = timerBeforeCall == null ? 0 : timerBeforeCall.count();

        evaluationProcessor.process(Long.MAX_VALUE);

        Timer timerAfterCall = meterRegistry.find("crackcs.evaluation.process").timer();
        assertThat(timerAfterCall).isNotNull();
        assertThat(timerAfterCall.count()).isEqualTo(countBeforeCall + 1);
    }
}
