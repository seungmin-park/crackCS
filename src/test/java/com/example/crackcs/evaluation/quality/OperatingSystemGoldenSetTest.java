package com.example.crackcs.evaluation.quality;

import com.example.crackcs.evaluation.domain.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatingSystemGoldenSetTest {

    @Test
    @DisplayName("독립 작성한 운영체제 골든 세트는 60개이며 필수 판정 유형을 포함한다")
    void containsSixtyOperatingSystemCases() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/evaluation/os-golden-set.tsv")) {
            assertThat(stream).isNotNull();
            List<String[]> cases = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines().skip(1).map(line -> line.split("\\t", -1)).toList();

            assertThat(cases).hasSize(60);
            assertThat(cases).allSatisfy(value -> assertThat(value).hasSize(5));
            assertThat(cases).extracting(value -> Verdict.valueOf(value[3]))
                    .contains(Verdict.CORRECT, Verdict.PARTIALLY_CORRECT, Verdict.INCORRECT, Verdict.NEEDS_REVIEW);
            assertThat(cases).extracting(value -> value[4])
                    .contains("PARAPHRASE", "FLUENT_WRONG", "INSUFFICIENT_EVIDENCE");
        }
    }
}
