package com.example.crackcs.performance;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.knowledge.chunk.repository.KnowledgeChunkRepository;
import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkService;
import com.example.crackcs.content.knowledge.domain.KnowledgeDocument;
import com.example.crackcs.content.knowledge.domain.KnowledgeSourceType;
import com.example.crackcs.content.knowledge.repository.KnowledgeDocumentRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.evaluation.service.EvaluationProcessor;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.answer.service.AnswerService;
import com.example.crackcs.learning.answer.service.result.AnswerResult;
import com.example.crackcs.learning.mastery.repository.AppliedEvaluationConceptRepository;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.learning.mastery.service.KnowledgeQueryService;
import com.example.crackcs.learning.recommendation.service.RecommendationService;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local-service-latency-benchmark")
@SpringBootTest(properties = "crackcs.evaluation.worker-enabled=false")
@ActiveProfiles("test")
class LocalServiceLatencyBenchmarkTest {

    private static final int FIXTURE_SIZE = 25;
    private static final int WARMUP_COUNT = 3;
    private static final int SAMPLE_COUNT = 20;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private EvaluationProcessor evaluationProcessor;

    @Autowired
    private KnowledgeQueryService knowledgeQueryService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    private KnowledgeChunkService knowledgeChunkService;

    @Autowired
    private AppliedEvaluationConceptRepository appliedEvaluationConceptRepository;

    @Autowired
    private KnowledgeStateRepository knowledgeStateRepository;

    @AfterEach
    void cleanUp() {
        appliedEvaluationConceptRepository.deleteAllInBatch();
        knowledgeStateRepository.deleteAllInBatch();
        evaluationRepository.deleteAll();
        answerRepository.deleteAllInBatch();
        questionRepository.deleteAll();
        knowledgeChunkRepository.deleteAllInBatch();
        knowledgeDocumentRepository.deleteAllInBatch();
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("고정 데이터에서 Service 조회와 평가 접수 및 완료의 로컬 p95를 기록한다")
    void writesLocalPerformanceBaseline() throws IOException {
        Member member = memberRepository.save(Member.builder().nickname("성능 학습자").build());
        Member admin = memberRepository.save(Member.builder().nickname("관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("PERF").name("성능 기준").build());
        Concept concept = conceptRepository.save(
                Concept.builder().topic(topic).code("PERF_CONCEPT").name("성능 개념").build());
        Question questionDraft = Question.builder()
                .topic(topic)
                .createdByMember(admin)
                .difficulty(QuestionDifficulty.BASIC)
                .content("성능 기준 문제")
                .referenceAnswer("성능 기준 답변")
                .build();
        questionDraft.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
        questionDraft.review(admin);
        questionDraft.publish();
        Question question = questionRepository.save(questionDraft);
        KnowledgeDocument document = KnowledgeDocument.builder()
                .topic(topic)
                .createdByMember(admin)
                .title("성능 근거")
                .sourceType(KnowledgeSourceType.INTERNAL_SUMMARY)
                .technologyVersion("general")
                .licenseNote("독립 작성")
                .content("성능 기준 답변은 성능 개념을 설명한다.")
                .build();
        document.review(admin);
        document.publish();
        document = knowledgeDocumentRepository.save(document);
        knowledgeChunkService.generateChunks(document.getId());

        AnswerResult evaluatedAnswer = submit(member, question, "기준 답변 0");
        evaluationProcessor.process(evaluatedAnswer.evaluationId());
        for (int index = 1; index < FIXTURE_SIZE; index++) {
            submit(member, question, "기준 답변 " + index);
        }

        Map<String, Measurement> measurements = new LinkedHashMap<>();
        measurements.put("answerHistory", measure(
                () -> answerService.findAll(member.getId(), PageRequest.of(0, FIXTURE_SIZE))));
        measurements.put("knowledgeMap", measure(
                () -> knowledgeQueryService.knowledgeStates(member.getId())));
        measurements.put("recommendation", measure(
                () -> recommendationService.recommendation(member.getId())));

        for (int index = 0; index < WARMUP_COUNT; index++) {
            AnswerResult warmup = submit(member, question, "접수 예열 " + index);
            evaluationProcessor.process(warmup.evaluationId());
        }
        List<Long> evaluationIds = new ArrayList<>();
        measurements.put("answerSubmission", measureSamples(() ->
                evaluationIds.add(submit(member, question, "접수 측정 " + evaluationIds.size()).evaluationId())));
        measurements.put("evaluationCompletion", measureSamples(new Runnable() {
            private int index;

            @Override
            public void run() {
                evaluationProcessor.process(evaluationIds.get(index));
                index++;
            }
        }));
        assertThat(evaluationRepository.findAllById(evaluationIds))
                .allMatch(evaluation -> evaluation.getStatus() == EvaluationStatus.EVALUATED);

        Path report = writeReport(measurements);

        String content = Files.readString(report, StandardCharsets.UTF_8);
        assertThat(content).contains(
                "\"fixtureSize\": 25",
                "\"warmupCount\": 3",
                "\"sampleCount\": 20",
                "\"medianMillis\"",
                "\"p95Millis\"",
                "\"answerSubmission\"",
                "\"evaluationCompletion\""
        );
    }

    private Measurement measure(Runnable operation) {
        for (int index = 0; index < WARMUP_COUNT; index++) {
            operation.run();
        }
        return measureSamples(operation);
    }

    private Measurement measureSamples(Runnable operation) {
        long[] nanos = new long[SAMPLE_COUNT];
        for (int index = 0; index < SAMPLE_COUNT; index++) {
            long started = System.nanoTime();
            operation.run();
            nanos[index] = System.nanoTime() - started;
        }
        Arrays.sort(nanos);
        return new Measurement(medianMillis(nanos), toMillis(nanos[p95Index()]));
    }

    private double medianMillis(long[] sortedNanos) {
        int middle = sortedNanos.length / 2;
        if (sortedNanos.length % 2 == 0) {
            return (toMillis(sortedNanos[middle - 1]) + toMillis(sortedNanos[middle])) / 2;
        }
        return toMillis(sortedNanos[middle]);
    }

    private int p95Index() {
        return Math.min(SAMPLE_COUNT - 1, (int) Math.ceil(SAMPLE_COUNT * 0.95) - 1);
    }

    private double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private Path writeReport(Map<String, Measurement> measurements) throws IOException {
        Path output = Path.of(System.getProperty(
                "benchmark.output",
                "build/reports/local-service-latency"
        ));
        Files.createDirectories(output);
        List<String> operationRows = measurements.entrySet().stream()
                .map(entry -> String.format(
                        Locale.ROOT,
                        "    \"%s\": {\"medianMillis\": %.3f, \"p95Millis\": %.3f}",
                        entry.getKey(), entry.getValue().medianMillis(), entry.getValue().p95Millis()
                ))
                .toList();
        String report = """
                {
                  "database": "H2 in-memory",
                  "fixtureSize": %d,
                  "warmupCount": %d,
                  "sampleCount": %d,
                  "operations": {
                %s
                  }
                }
                """.formatted(FIXTURE_SIZE, WARMUP_COUNT, SAMPLE_COUNT, String.join(",\n", operationRows));
        Path file = output.resolve("baseline.json");
        Files.writeString(file, report, StandardCharsets.UTF_8);
        return file;
    }

    private AnswerResult submit(Member member, Question question, String content) {
        return answerService.submit(
                member.getId(),
                question.getId(),
                UUID.randomUUID().toString(),
                content
        );
    }

    private record Measurement(double medianMillis, double p95Millis) {
    }
}
