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
    @Autowired KnowledgeRetrievalService retrieval;
    @Autowired KnowledgeChunkService chunkService;
    @Autowired KnowledgeChunkRepository chunks;
    @Autowired KnowledgeDocumentRepository documents;
    @Autowired TopicRepository topics;
    @Autowired MemberRepository members;
    @Autowired DataSource dataSource;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path reference = Path.of(System.getProperty(
            "reference.directory", "docs/evaluation/reference-v1"));
    private final Path referenceData = reference.resolve("data");

    @AfterEach
    void tearDown() {
        chunks.deleteAllInBatch();
        documents.deleteAllInBatch();
        topics.deleteAllInBatch();
        members.deleteAllInBatch();
    }

    @Test
    @DisplayName("확정 자료를 실제 분할과 DB ID로 연결해 검색 기준선을 측정한다")
    void measuresFrozenReferenceWithProductionChunkingAndRetrieval() throws Exception {
        JsonNode standard = mapper.readTree(reference.resolve("manifest.json").toFile());
        // Also protect direct IDE execution, which does not run Gradle's preflight task.
        for (String filename : List.of("questions.jsonl", "golden-set.jsonl", "knowledge-documents.jsonl",
                "sources.json", "experiment-splits.json")) {
            String actualHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(referenceData.resolve(filename))));
            assertThat(actualHash).as("확정 이후 변경된 자료 %s", filename)
                    .isEqualTo(standard.get("artifacts").get(filename).asText());
        }
        List<JsonNode> questionRows = readRows("questions.jsonl");
        Map<String, JsonNode> questionById = new LinkedHashMap<>();
        Map<String, Long> topicIds = new LinkedHashMap<>();
        for (JsonNode question : questionRows) {
            questionById.put(question.get("id").asText(), question);
            String topicKey = question.get("topicKey").asText();
            if (!topicIds.containsKey(topicKey)) {
                Topic saved = topics.save(Topic.builder().code(topicKey).name(topicKey).build());
                topicIds.put(topicKey, saved.getId());
            }
        }
        // This is test-only publication to exercise production filters, not human review of the source files.
        Member admin = members.save(Member.builder().nickname("검색 실험 전용 관리자")
                .role(MemberRole.ADMIN).build());
        Map<String, Set<Long>> evidenceIds = new LinkedHashMap<>();
        Map<String, Object> documentMappings = new LinkedHashMap<>();
        Set<String> chunkPolicies = new LinkedHashSet<>();
        for (JsonNode document : readRows("knowledge-documents.jsonl")) {
            String content = String.join("\n", texts(document.get("chunks"), "content"));
            Topic topic = topics.findById(topicIds.get(document.get("topicKey").asText())).orElseThrow();
            KnowledgeDocument saved = documents.save(KnowledgeDocument.builder()
                    .topic(topic).createdByMember(admin).title(document.get("title").asText())
                    .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                    .technologyVersion(document.get("technologyVersion").asText())
                    .licenseNote("격리된 테스트 전용; 정답 기준의 독립 사람 검수나 운영 공개 아님")
                    .content(content).build());
            saved.review(admin);
            saved.publish();
            documents.save(saved);
            chunkService.generateChunks(saved.getId());
            List<KnowledgeChunk> stored = chunkService.findByDocumentId(saved.getId());
            List<ReferenceChunkMapping.StoredChunk> spans = stored.stream()
                    .map(chunk -> new ReferenceChunkMapping.StoredChunk(saved.getId(), chunk.getId(),
                            chunk.getStartOffset(), chunk.getEndOffset(), chunk.getContent())).toList();
            List<ReferenceChunkMapping.Evidence> expected = new ArrayList<>();
            int offset = 0;
            for (JsonNode evidence : document.get("chunks")) {
                String body = evidence.get("content").asText();
                expected.add(new ReferenceChunkMapping.Evidence(evidence.get("id").asText(),
                        offset, offset + body.length(), body));
                offset += body.length() + 1;
            }
            evidenceIds.putAll(ReferenceChunkMapping.connect(saved.getId(), content, expected, spans));
            documentMappings.put(document.get("id").asText(), Map.of(
                    "documentId", saved.getId(), "checksum", saved.getChecksum(), "chunks", spans));
            stored.forEach(chunk -> chunkPolicies.add(chunk.getChunkPolicyVersion()));
        }
        assertThat(documents.count()).isEqualTo(60);
        assertThat(evidenceIds).hasSize(120);
        assertThat(evidenceIds.values()).allSatisfy(ids -> assertThat(ids).isNotEmpty());

        Map<String, String> splits = readSplits();
        List<CaseResult> results = new ArrayList<>();
        List<JsonNode> cases = readRows("golden-set.jsonl").stream()
                .filter(row -> !row.get("caseType").asText().equals("INSUFFICIENT_EVIDENCE")).toList();
        assertThat(cases).hasSize(180);
        for (int k : List.of(1, 3, 5)) {
            for (JsonNode golden : cases) {
                JsonNode question = questionById.get(golden.get("questionId").asText());
                String topicKey = question.get("topicKey").asText();
                RetrievalQuery query = new RetrievalQuery(topicIds.get(topicKey),
                        texts(question.get("concepts"), "name"), question.get("content").asText(),
                        question.get("referenceAnswer").asText(), golden.get("answer").asText());
                Set<Long> relevant = new LinkedHashSet<>();
                for (JsonNode key : golden.get("providedEvidenceIds")) {
                    Set<Long> mapped = evidenceIds.get(key.asText());
                    assertThat(mapped).as("알 수 없는 근거 %s", key.asText()).isNotNull();
                    relevant.addAll(mapped);
                }
                RetrievalResult found = retrieval.retrieve(query, k);
                List<Long> retrieved = found.chunks().stream().map(row -> row.chunk().getId()).toList();
                assertThat(retrieved).doesNotHaveDuplicates().hasSizeLessThanOrEqualTo(k);
                assertThat(found.chunks()).allSatisfy(row -> assertThat(row.chunk().getDocument().getTopicId())
                        .isEqualTo(topicIds.get(topicKey)));
                assertThat(found.insufficientEvidence()).isEqualTo(retrieved.isEmpty());
                assertThat(retrieval.retrieve(query, k).chunks().stream().map(row -> row.chunk().getId()).toList())
                        .containsExactlyElementsOf(retrieved);
                results.add(new CaseResult(golden.get("id").asText(), question.get("id").asText(),
                        topicKey, splits.get(question.get("id").asText()), golden.get("caseType").asText(),
                        k, relevant.stream().sorted().toList(), retrieved,
                        RetrievalQualityMetrics.calculate(relevant, retrieved), found.conflictingEvidence()));
            }
        }
        assertThat(results).hasSize(540);
        String database;
        try (Connection connection = dataSource.getConnection()) {
            database = connection.getMetaData().getDatabaseProductName();
        }
        assertThat(database).isEqualTo(System.getProperty("benchmark.database", "H2"));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("measuredAt", Instant.now().toString());
        report.put("referenceVersion", standard.get("version").asText());
        report.put("referenceArtifacts", standard.get("artifacts"));
        report.put("database", database);
        report.put("conceptInput", "questions.jsonl concepts.name (same field as production)");
        report.put("chunkPolicies", chunkPolicies);
        report.put("documents", documents.count());
        report.put("referenceEvidenceSpans", evidenceIds.size());
        report.put("persistedChunks", chunks.count());
        report.put("aiCalls", 0);
        report.put("independentBenchmark", false);
        report.put("irrelevancePolicy", "not in mapped reference IDs; cross-question relevance not independently annotated");
        report.put("summaries", summaries(results));
        report.put("evidenceMapping", evidenceIds);
        report.put("documentMapping", documentMappings);
        report.put("cases", results);
        Path output = Path.of(System.getProperty("benchmark.output", "build/reports/retrieval/h2"));
        Files.createDirectories(output);
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("baseline.json").toFile(), report);
        System.out.println("Retrieval measurement: " + mapper.writeValueAsString(report.get("summaries")));
    }

    private List<JsonNode> readRows(String filename) throws Exception {
        return Files.readAllLines(referenceData.resolve(filename)).stream().filter(line -> !line.isBlank())
                .map(mapper::readTree).toList();
    }

    private List<String> texts(JsonNode rows, String field) {
        List<String> result = new ArrayList<>();
        for (JsonNode row : rows) {
            result.add(row.get(field).asText());
        }
        return result;
    }

    private Map<String, String> readSplits() {
        JsonNode manifest = mapper.readTree(referenceData.resolve("experiment-splits.json").toFile());
        Map<String, String> splits = new LinkedHashMap<>();
        for (JsonNode group : manifest.get("groups")) {
            for (JsonNode id : group.get("questionIds")) {
                splits.put(id.asText(), group.get("split").asText());
            }
        }
        return splits;
    }

    private List<Map<String, Object>> summaries(List<CaseResult> results) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (int k : List.of(1, 3, 5)) {
            for (String scope : List.of("all", "development", "evaluation-candidate", "CS", "JAVA", "SPRING", "JPA", "AX")) {
                List<CaseResult> selected = results.stream().filter(row -> row.k() == k)
                        .filter(row -> scope.equals("all") || scope.equals(row.split()) || scope.equals(row.topic())).toList();
                RetrievalBenchmarkStatistics statistics = RetrievalBenchmarkStatistics.summarize(selected.stream()
                        .map(row -> new RetrievalBenchmarkStatistics.Observation(row.metrics(), row.retrievedIds().size())).toList());
                summaries.add(Map.of("k", k, "scope", scope, "statistics", statistics,
                        "conflictingEvidenceQueries", selected.stream().filter(CaseResult::conflictingEvidence).count()));
            }
        }
        return summaries;
    }

    record CaseResult(String caseId, String questionId, String topic, String split, String caseType, int k,
                      List<Long> relevantIds, List<Long> retrievedIds, RetrievalQualityMetrics metrics,
                      boolean conflictingEvidence) {}
}
