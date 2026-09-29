package com.example.crackcs.evaluation.adapter.openai;

import com.example.crackcs.evaluation.domain.EvaluationResult;
import com.example.crackcs.evaluation.domain.Verdict;
import com.example.crackcs.evaluation.port.EvaluationRequest;
import com.example.crackcs.evaluation.quality.GoldenSetMetrics;
import com.example.crackcs.exception.ProviderRequestRejectedException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("openai-live")
class OpenAiLiveEvaluationTest {

    @Test
    @DisplayName("유료 API에 제한된 정답 표본을 보내 판정·근거·비용·시간을 기록한다")
    void measuresReferenceCases() throws IOException, InterruptedException {
        String apiKey = System.getenv("OPENAI_API_KEY");
        assertThat(apiKey).as("OPENAI_API_KEY must be set in the test runner's terminal").isNotBlank();
        ObjectMapper objectMapper = new ObjectMapper();
        Path referenceDirectory = Path.of(System.getProperty("reference.directory"));
        String split = System.getProperty("openai.split");
        String model = System.getProperty("openai.model");
        assertThat(model).as("pricing and local cap are calibrated for this model").isEqualTo("gpt-5.6-terra");
        String reasoningEffort = System.getProperty("openai.reasoning.effort");
        int maxOutputTokens = Integer.parseInt(System.getProperty("openai.max.output.tokens"));
        int caseLimit = Integer.parseInt(System.getProperty("openai.case.limit"));
        Set<String> requestedCaseIds = Arrays.stream(System.getProperty("openai.case.ids").split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toSet());
        boolean includeFeedback = Boolean.parseBoolean(System.getProperty("openai.include.feedback"));
        double maxSpendUsd = Double.parseDouble(System.getProperty("openai.max.spend.usd"));
        assertThat(caseLimit).isPositive();
        assertThat(maxSpendUsd).isPositive();
        if (!requestedCaseIds.isEmpty()) {
            assertThat(requestedCaseIds).hasSize(caseLimit);
        }

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
                .filter(row -> requestedCaseIds.isEmpty()
                        || requestedCaseIds.contains(row.path("caseId").stringValue()))
                .limit(caseLimit)
                .toList();
        assertThat(sample).hasSize(caseLimit);

        OpenAiEvaluationAdapter adapter = new OpenAiEvaluationAdapter(
                new JdkOpenAiResponsesClient(apiKey, URI.create("https://api.openai.com/v1/responses")),
                new OpenAiEvaluationRequestFactory(objectMapper, model, reasoningEffort, maxOutputTokens),
                new OpenAiEvaluationResponseParser(objectMapper, model, "os-evaluator-v3"),
                Duration.ofSeconds(60)
        );
        ObjectNode report = objectMapper.createObjectNode();
        report.put("model", model);
        report.put("reasoningEffort", reasoningEffort);
        report.put("maxOutputTokens", maxOutputTokens);
        report.put("evaluatorVersion", "os-evaluator-v3");
        report.put("referenceVersion", objectMapper.readTree(
                Files.readString(referenceDirectory.resolve("manifest.json"))).path("version").stringValue());
        report.put("split", split);
        report.put("scope", "provider cases only; insufficient-evidence cases stop before model invocation");
        report.put("spendEstimate", "standard gpt-5.6-terra text rates; verify billed cost in Platform");
        report.put("maxSpendUsd", maxSpendUsd);
        report.put("includesFeedback", includeFeedback);
        ArrayNode cases = report.putArray("cases");
        List<Long> durations = new ArrayList<>();
        List<GoldenSetMetrics.Observation> observations = new ArrayList<>();
        int completed = 0;
        int invalidEvidence = 0;
        double estimatedUsd = 0;

        Path reportPath = Path.of(System.getProperty("benchmark.output"));
        Files.createDirectories(reportPath.getParent());
        Path progressPath = reportPath.resolveSibling(reportPath.getFileName() + ".partial.json");

        for (JsonNode input : sample) {
            if (estimatedUsd >= maxSpendUsd) {
                report.put("stoppedReason", "LOCAL_SPEND_ESTIMATE_REACHED");
                break;
            }
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
                double caseCostUsd = result.inputTokens() * 2.0 / 1_000_000
                        + result.outputTokens() * 12.0 / 1_000_000;
                estimatedUsd += caseCostUsd;
                measuredCase.put("actualVerdict", result.verdict().name())
                        .put("evidenceValid", evidenceValid)
                        .put("inputTokens", result.inputTokens())
                        .put("outputTokens", result.outputTokens())
                        .put("estimatedUsd", caseCostUsd);
                if (includeFeedback && !expectedVerdict.equals(result.verdict().name())) {
                    measuredCase.put("feedback", result.feedback());
                    measuredCase.set("concepts", objectMapper.valueToTree(result.concepts()));
                }
            } catch (RuntimeException failure) {
                measuredCase.put("errorType", failure.getClass().getSimpleName());
                if (failure instanceof ProviderRequestRejectedException) {
                    report.put("stoppedReason", "NON_RETRYABLE_PROVIDER_REJECTION");
                }
            }
            long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
            durations.add(durationMillis);
            measuredCase.put("durationMillis", durationMillis);
            report.put("measurementComplete", false);
            report.put("attempted", cases.size());
            report.put("completed", completed);
            report.put("estimatedUsd", estimatedUsd);
            Files.writeString(progressPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
                    StandardCharsets.UTF_8);
            System.out.printf("OpenAI case=%s durationMillis=%d completed=%d attempted=%d estimatedUsd=%.5f%n",
                    caseId, durationMillis, completed, cases.size(), estimatedUsd);
            if (report.has("stoppedReason")) {
                break;
            }
        }

        Collections.sort(durations);
        report.put("caseCount", sample.size());
        report.put("completed", completed);
        report.put("failed", cases.size() - completed);
        report.put("invalidEvidence", invalidEvidence);
        report.put("schemaSuccessRate", cases.isEmpty() ? 0 : (double) completed / cases.size());
        if (!durations.isEmpty()) {
            report.put("p95Millis", durations.get((int) Math.ceil(durations.size() * 0.95) - 1));
        }
        if (!observations.isEmpty()) {
            GoldenSetMetrics.Result metrics = GoldenSetMetrics.calculate(observations);
            putRatio(report, "detailedAgreement", metrics.detailedAgreement());
            putRatio(report, "binaryAgreement", metrics.binaryAgreement());
            putRatio(report, "falseCorrectRate", metrics.falseCorrectRate());
        }
        report.put("measurementComplete", true);
        Files.writeString(reportPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
                StandardCharsets.UTF_8);
        assertThat(completed).as("OpenAI calls completed; see " + reportPath).isEqualTo(sample.size());
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
