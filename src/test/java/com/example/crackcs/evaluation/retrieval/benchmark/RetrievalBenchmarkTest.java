package com.example.crackcs.evaluation.retrieval.benchmark;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkService;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.retrieval.KnowledgeRetrievalService;
import com.example.crackcs.evaluation.retrieval.RetrievalQualityMetrics;
import com.example.crackcs.evaluation.retrieval.RetrievalQuery;
import com.example.crackcs.evaluation.retrieval.RetrievalResult;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("retrieval-benchmark")
@SpringBootTest(properties = {"crackcs.evaluation.worker-enabled=false", "crackcs.followup.worker-enabled=false"})
class RetrievalBenchmarkTest {
    @Autowired KnowledgeRetrievalService knowledgeRetrievalService;
    @Autowired KnowledgeChunkService knowledgeChunkService;
    @Autowired KnowledgeChunkRepository knowledgeChunkRepository;
    @Autowired KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path reference = Path.of(System.getProperty(
            "reference.directory", "docs/evaluation/reference-v1"));
    private final Path referenceData = reference.resolve("data");

    @AfterEach
    void tearDown() {
        knowledgeChunkRepository.deleteAllInBatch();
        knowledgeDocumentRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("확정 자료를 실제 분할과 DB ID로 연결해 검색 기준선을 측정한다")
    void measuresFrozenReferenceWithProductionChunkingAndRetrieval() throws Exception {
        JsonNode referenceManifest = objectMapper.readTree(reference.resolve("manifest.json").toFile());
        // Also protect direct IDE execution, which does not run Gradle's preflight task.
        for (String filename : List.of("questions.jsonl", "golden-set.jsonl", "knowledge-documents.jsonl",
                "sources.json", "experiment-splits.json")) {
            String actualHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(referenceData.resolve(filename))));
            assertThat(actualHash).as("확정 이후 변경된 자료 %s", filename)
                    .isEqualTo(referenceManifest.get("artifacts").get(filename).asText());
        }
        List<JsonNode> referenceQuestions = readRows("questions.jsonl");
        Map<String, JsonNode> questionById = new LinkedHashMap<>();
        Map<String, Long> topicIds = new LinkedHashMap<>();
        for (JsonNode question : referenceQuestions) {
            questionById.put(question.get("id").asText(), question);
            String topicKey = question.get("topicKey").asText();
            if (!topicIds.containsKey(topicKey)) {
                Topic savedTopic = topicRepository.save(Topic.builder().code(topicKey).name(topicKey).build());
                topicIds.put(topicKey, savedTopic.getId());
            }
        }
        // This is test-only publication to exercise production filters, not human review of the source files.
        Member benchmarkAdmin = memberRepository.save(Member.builder().nickname("검색 실험 전용 관리자")
                .role(MemberRole.ADMIN).build());
        Map<String, Set<Long>> chunkIdsByEvidenceId = new LinkedHashMap<>();
        Map<String, Object> storedDocumentMappings = new LinkedHashMap<>();
        Set<String> chunkPolicies = new LinkedHashSet<>();
        for (JsonNode document : readRows("knowledge-documents.jsonl")) {
            String content = String.join("\n", texts(document.get("chunks"), "content"));
            Topic topic = topicRepository.findById(topicIds.get(document.get("topicKey").asText())).orElseThrow();
            KnowledgeDocument savedDocument = knowledgeDocumentRepository.save(KnowledgeDocument.builder()
                    .topic(topic).createdByMember(benchmarkAdmin).title(document.get("title").asText())
                    .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                    .technologyVersion(document.get("technologyVersion").asText())
                    .licenseNote("격리된 테스트 전용; 정답 기준의 독립 사람 검수나 운영 공개 아님")
                    .content(content).build());
            savedDocument.review(benchmarkAdmin);
            savedDocument.publish();
            knowledgeDocumentRepository.save(savedDocument);
            knowledgeChunkService.generateChunks(savedDocument.getId());
            List<KnowledgeChunk> storedChunks = knowledgeChunkService.findByDocumentId(savedDocument.getId());
            List<ReferenceChunkMapping.StoredChunk> storedChunkSpans = storedChunks.stream()
                    .map(chunk -> new ReferenceChunkMapping.StoredChunk(savedDocument.getId(), chunk.getId(),
                            chunk.getStartOffset(), chunk.getEndOffset(), chunk.getContent())).toList();
            List<ReferenceChunkMapping.Evidence> referenceEvidence = new ArrayList<>();
            int offset = 0;
            for (JsonNode evidence : document.get("chunks")) {
                String body = evidence.get("content").asText();
                referenceEvidence.add(new ReferenceChunkMapping.Evidence(evidence.get("id").asText(),
                        offset, offset + body.length(), body));
                offset += body.length() + 1;
            }
            chunkIdsByEvidenceId.putAll(ReferenceChunkMapping.connect(
                    savedDocument.getId(), content, referenceEvidence, storedChunkSpans));
            storedDocumentMappings.put(document.get("id").asText(), Map.of(
                    "documentId", savedDocument.getId(), "checksum", savedDocument.getChecksum(),
                    "chunks", storedChunkSpans));
            storedChunks.forEach(chunk -> chunkPolicies.add(chunk.getChunkPolicyVersion()));
        }
        assertThat(knowledgeDocumentRepository.count()).isEqualTo(60);
        assertThat(chunkIdsByEvidenceId).hasSize(120);
        assertThat(chunkIdsByEvidenceId.values()).allSatisfy(ids -> assertThat(ids).isNotEmpty());

        Map<String, String> splitByQuestionId = readSplits();
        List<CaseResult> caseResults = new ArrayList<>();
        List<JsonNode> goldenCases = readRows("golden-set.jsonl").stream()
                .filter(row -> !row.get("caseType").asText().equals("INSUFFICIENT_EVIDENCE")).toList();
        assertThat(goldenCases).hasSize(180);
        for (int k : List.of(1, 3, 5)) {
            for (JsonNode goldenCase : goldenCases) {
                JsonNode question = questionById.get(goldenCase.get("questionId").asText());
                String topicKey = question.get("topicKey").asText();
                RetrievalQuery retrievalQuery = new RetrievalQuery(topicIds.get(topicKey),
                        texts(question.get("concepts"), "name"), question.get("content").asText(),
                        question.get("referenceAnswer").asText(), goldenCase.get("answer").asText());
                Set<Long> relevantChunkIds = new LinkedHashSet<>();
                for (JsonNode key : goldenCase.get("providedEvidenceIds")) {
                    Set<Long> mappedChunkIds = chunkIdsByEvidenceId.get(key.asText());
                    assertThat(mappedChunkIds).as("알 수 없는 근거 %s", key.asText()).isNotNull();
                    relevantChunkIds.addAll(mappedChunkIds);
                }
                RetrievalResult retrievalResult = knowledgeRetrievalService.retrieve(retrievalQuery, k);
                List<Long> retrievedChunkIds = retrievalResult.chunks().stream()
                        .map(row -> row.chunk().getId()).toList();
                assertThat(retrievedChunkIds).doesNotHaveDuplicates().hasSizeLessThanOrEqualTo(k);
                assertThat(retrievalResult.chunks()).allSatisfy(row -> assertThat(row.chunk().getDocument().getTopicId())
                        .isEqualTo(topicIds.get(topicKey)));
                assertThat(retrievalResult.insufficientEvidence()).isEqualTo(retrievedChunkIds.isEmpty());
                assertThat(knowledgeRetrievalService.retrieve(retrievalQuery, k).chunks().stream()
                        .map(row -> row.chunk().getId()).toList())
                        .containsExactlyElementsOf(retrievedChunkIds);
                caseResults.add(new CaseResult(goldenCase.get("id").asText(), question.get("id").asText(),
                        topicKey, splitByQuestionId.get(question.get("id").asText()),
                        goldenCase.get("caseType").asText(), k, relevantChunkIds.stream().sorted().toList(),
                        retrievedChunkIds, RetrievalQualityMetrics.calculate(relevantChunkIds, retrievedChunkIds),
                        retrievalResult.conflictingEvidence()));
            }
        }
        assertThat(caseResults).hasSize(540);
        String databaseProductName;
        try (Connection connection = dataSource.getConnection()) {
            databaseProductName = connection.getMetaData().getDatabaseProductName();
        }
        assertThat(databaseProductName).isEqualTo(System.getProperty("benchmark.database", "H2"));
        Map<String, Object> benchmarkReport = new LinkedHashMap<>();
        benchmarkReport.put("measuredAt", Instant.now().toString());
        benchmarkReport.put("referenceVersion", referenceManifest.get("version").asText());
        benchmarkReport.put("referenceArtifacts", referenceManifest.get("artifacts"));
        benchmarkReport.put("database", databaseProductName);
        benchmarkReport.put("conceptInput", "questions.jsonl concepts.name (same field as production)");
        benchmarkReport.put("chunkPolicies", chunkPolicies);
        benchmarkReport.put("documents", knowledgeDocumentRepository.count());
        benchmarkReport.put("referenceEvidenceSpans", chunkIdsByEvidenceId.size());
        benchmarkReport.put("persistedChunks", knowledgeChunkRepository.count());
        benchmarkReport.put("aiCalls", 0);
        benchmarkReport.put("independentBenchmark", false);
        benchmarkReport.put("irrelevancePolicy", "not in mapped reference IDs; cross-question relevance not independently annotated");
        benchmarkReport.put("summaries", summaries(caseResults));
        benchmarkReport.put("evidenceMapping", chunkIdsByEvidenceId);
        benchmarkReport.put("documentMapping", storedDocumentMappings);
        benchmarkReport.put("cases", caseResults);
        Path benchmarkOutput = Path.of(System.getProperty("benchmark.output", "build/reports/retrieval/h2"));
        Files.createDirectories(benchmarkOutput);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(benchmarkOutput.resolve("baseline.json").toFile(), benchmarkReport);
        System.out.println("Retrieval measurement: "
                + objectMapper.writeValueAsString(benchmarkReport.get("summaries")));
    }

    private List<JsonNode> readRows(String filename) throws Exception {
        return Files.readAllLines(referenceData.resolve(filename)).stream().filter(line -> !line.isBlank())
                .map(objectMapper::readTree).toList();
    }

    private List<String> texts(JsonNode nodes, String field) {
        List<String> texts = new ArrayList<>();
        for (JsonNode node : nodes) {
            texts.add(node.get(field).asText());
        }
        return texts;
    }

    private Map<String, String> readSplits() {
        JsonNode manifest = objectMapper.readTree(referenceData.resolve("experiment-splits.json").toFile());
        Map<String, String> splits = new LinkedHashMap<>();
        for (JsonNode group : manifest.get("groups")) {
            for (JsonNode id : group.get("questionIds")) {
                splits.put(id.asText(), group.get("split").asText());
            }
        }
        return splits;
    }

    private List<Map<String, Object>> summaries(List<CaseResult> caseResults) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (int k : List.of(1, 3, 5)) {
            for (String scope : List.of("all", "development", "evaluation-candidate", "CS", "JAVA", "SPRING", "JPA", "AX")) {
                List<CaseResult> scopedCaseResults = caseResults.stream().filter(row -> row.k() == k)
                        .filter(row -> scope.equals("all") || scope.equals(row.split()) || scope.equals(row.topic())).toList();
                RetrievalBenchmarkStatistics statistics = RetrievalBenchmarkStatistics.summarize(scopedCaseResults.stream()
                        .map(row -> new RetrievalBenchmarkStatistics.Observation(row.metrics(), row.retrievedIds().size())).toList());
                summaries.add(Map.of("k", k, "scope", scope, "statistics", statistics,
                        "conflictingEvidenceQueries",
                        scopedCaseResults.stream().filter(CaseResult::conflictingEvidence).count()));
            }
        }
        return summaries;
    }

    record CaseResult(String caseId, String questionId, String topic, String split, String caseType, int k,
                      List<Long> relevantIds, List<Long> retrievedIds, RetrievalQualityMetrics metrics,
                      boolean conflictingEvidence) {}
}
