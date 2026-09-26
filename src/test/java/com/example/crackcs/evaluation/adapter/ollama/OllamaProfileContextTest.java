package com.example.crackcs.evaluation.adapter.ollama;

import com.example.crackcs.evaluation.port.EvaluationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "crackcs.evaluation.worker-enabled=false",
        "crackcs.followup.worker-enabled=false"
})
@ActiveProfiles({"test", "ollama"})
class OllamaProfileContextTest {

    @Autowired
    private EvaluationPort evaluationPort;

    @Test
    @DisplayName("test와 ollama profile을 함께 켜면 로컬 평가 어댑터를 사용한다")
    void selectsOllamaFromProfile() {
        assertThat(evaluationPort).isInstanceOf(OllamaEvaluationAdapter.class);
    }
}
