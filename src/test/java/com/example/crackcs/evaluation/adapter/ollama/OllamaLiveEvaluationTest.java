package com.example.crackcs.evaluation.adapter.ollama;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.quality.GoldenSetMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ollama-live")
class OllamaLiveEvaluationTest {

    @Test
    @DisplayName("확정된 평가 입력을 로컬 모델에 보내 판정과 근거·시간을 기록한다")
    void measuresReferenceCases() throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        Path referenceDirectory = Path.of(System.getProperty("reference.directory"));
        String split = System.getProperty("ollama.split");
        String reasoningEffort = System.getProperty("ollama.reasoning.effort");
        int caseLimit = Integer.parseInt(System.getProperty("ollama.case.limit"));
        assertThat(caseLimit).isPositive();
        List<JsonNode> inputs = export(referenceDirectory, "inputs", split, objectMapper);
        List<JsonNode> labels = export(referenceDirectory, "labels", split, objectMapper);
        Map<String, String> expectedVerdicts = labels.stream().collect(Collectors.toMap(
                row -> row.path("caseId").stringValue(),
                row -> row.path("expectedVerdict").stringValue()
        ));
        Set<String> providerCaseIds = labels.stream()
                .filter(row -> !"INSUFFICIENT_EVIDENCE".equals(row.path("caseType").stringValue()))
                .map(row -> row.path("caseId").stringValue())
                .collect(Collectors.toSet());
        List<JsonNode> sample = inputs.stream()
                .filter(row -> providerCaseIds.contains(row.path("caseId").stringValue()))
                .limit(caseLimit)
                .toList();
        assertThat(sample).hasSize(caseLimit);
        OllamaEvaluationAdapter adapter = new OllamaEvaluationAdapter(
                objectMapper, URI.create("http://127.0.0.1:11434/api/chat"),
                "gpt-oss:20b", "os-evaluator-v1", reasoningEffort, Duration.ofMinutes(3)
        );
        ObjectNode report = objectMapper.createObjectNode();
        report.put("model", "gpt-oss:20b");
        report.put("evaluatorVersion", "os-evaluator-v1-" + reasoningEffort);
        report.put("reasoningEffort", reasoningEffort);
        report.put("referenceVersion", objectMapper.readTree(
                Files.readString(referenceDirectory.resolve("manifest.json"))).path("version").stringValue());
        report.put("split", split);
        report.put("scope", "provider cases only; insufficient-evidence cases stop before model invocation");
        ArrayNode cases = report.putArray("cases");
        List<Long> durations = new ArrayList<>();
        List<GoldenSetMetrics.Observation> observations = new ArrayList<>();
        int completed = 0;
        int invalidEvidence = 0;

        for (JsonNode input : sample) {
            String caseId = input.path("caseId").stringValue();
            EvaluationRequest request = objectMapper.treeToValue(input.path("request"), EvaluationRequest.class);
            String expectedVerdict = expectedVerdicts.get(caseId);
            assertThat(expectedVerdict).isNotBlank();
            ObjectNode measuredCase = cases.addObject()
                    .put("caseId", caseId)
                    .put("expectedVerdict", expectedVerdict);
            long started = System.nanoTime();
            try {
                EvaluationResult result = adapter.evaluate(request);
                Set<Long> suppliedIds = request.evidence().stream()
                        .map(evidence -> evidence.chunkId())
                        .collect(Collectors.toSet());
                boolean evidenceValid = !result.evidenceChunkIds().isEmpty()
                        && Set.copyOf(result.evidenceChunkIds()).size() == result.evidenceChunkIds().size()
                        && suppliedIds.containsAll(result.evidenceChunkIds());
                if (!evidenceValid) {
                    invalidEvidence++;
                }
                observations.add(new GoldenSetMetrics.Observation(Verdict.valueOf(expectedVerdict), result.verdict()));
                completed++;
                measuredCase.put("actualVerdict", result.verdict().name())
                        .put("evidenceValid", evidenceValid)
                        .put("inputTokens", result.inputTokens())
                        .put("outputTokens", result.outputTokens());
            } catch (RuntimeException failure) {
                measuredCase.put("errorType", failure.getClass().getSimpleName());
            }
            long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
            durations.add(durationMillis);
            measuredCase.put("durationMillis", durationMillis);
        }

        Collections.sort(durations);
        report.put("caseCount", sample.size());
        report.put("completed", completed);
        report.put("failed", sample.size() - completed);
        report.put("invalidEvidence", invalidEvidence);
        report.put("schemaSuccessRate", (double) completed / sample.size());
        report.put("p95Millis", durations.get((int) Math.ceil(durations.size() * 0.95) - 1));
        if (!observations.isEmpty()) {
            GoldenSetMetrics.Result metrics = GoldenSetMetrics.calculate(observations);
            putRatio(report, "detailedAgreement", metrics.detailedAgreement());
            putRatio(report, "binaryAgreement", metrics.binaryAgreement());
            putRatio(report, "falseCorrectRate", metrics.falseCorrectRate());
        }

        Path reportPath = Path.of(System.getProperty("benchmark.output"));
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
                StandardCharsets.UTF_8);
        assertThat(completed).as("local model calls completed; see " + reportPath).isEqualTo(sample.size());
        assertThat(invalidEvidence).as("invalid evidence references; see " + reportPath).isZero();
    }

    private void putRatio(ObjectNode report, String field, Double value) {
        if (value == null) {
            report.putNull(field);
        } else {
            report.put(field, value);
        }
    }

    private List<JsonNode> export(Path referenceDirectory, String kind, String split, ObjectMapper objectMapper)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "python3", referenceDirectory.resolve("tools/bundle.py").toString(),
                "--export", kind, "--split", split
        ).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        assertThat(exitCode).as(errors).isZero();
        return output.lines().filter(line -> !line.isBlank()).map(objectMapper::readTree).toList();
    }
}
