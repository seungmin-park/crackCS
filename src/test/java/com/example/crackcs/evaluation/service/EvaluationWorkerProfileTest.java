package com.example.crackcs.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class EvaluationWorkerProfileTest {

    @Test
    @DisplayName("평가 Worker는 운영을 포함한 모든 profile에서 사용할 수 있다")
    void workerIsNotRestrictedToDevelopmentProfiles() {
        assertThat(EvaluationWorker.class.getAnnotation(Profile.class)).isNull();
    }
}
