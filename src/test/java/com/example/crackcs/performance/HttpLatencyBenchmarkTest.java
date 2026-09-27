package com.example.crackcs.performance;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.repository.AuthAccountRepository;
import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.concept.repository.ConceptRepository;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionConceptAssignment;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.content.topic.repository.TopicRepository;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.repository.MemberRepository;
import com.example.crackcs.support.ConcurrentRequests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("http-latency-benchmark")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0", "crackcs.evaluation.worker-enabled=false", "crackcs.followup.worker-enabled=false"
})
@ActiveProfiles("test")
class HttpLatencyBenchmarkTest {
    @Value("${local.server.port}") private int port;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private ConceptRepository conceptRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private EvaluationRepository evaluationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        evaluationRepository.deleteAll();
        answerRepository.deleteAllInBatch();
        questionRepository.deleteAll();
        conceptRepository.deleteAllInBatch();
        topicRepository.deleteAllInBatch();
        authAccountRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("PostgreSQL과 실제 HTTP 서버에서 조회·평가 접수의 시간과 성공 응답을 기록한다")
    void measuresAuthenticatedHttpRequests() throws Exception {
        Member admin = memberRepository.save(Member.builder().nickname("HTTP 관리자").role(MemberRole.ADMIN).build());
        Topic topic = topicRepository.save(Topic.builder().code("HTTP_PERF").name("HTTP 측정").build());
        Concept concept = conceptRepository.save(Concept.builder().topic(topic).code("HTTP_CONCEPT").name("스레드").build());
        List<Question> questions = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            Question question = Question.builder().topic(topic).createdByMember(admin).difficulty(QuestionDifficulty.BASIC)
                    .content("스레드 설명 " + index).referenceAnswer("프로세스 자원을 공유하는 실행 단위").build();
            question.replaceConcepts(List.of(new QuestionConceptAssignment(concept, BigDecimal.ONE, true)));
            question.review(admin);
            question.publish();
            questions.add(questionRepository.save(question));
        }
        List<AuthenticatedHttpClient> clients = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            Member learner = memberRepository.save(Member.builder().nickname("HTTP 학습자 " + index).build());
            String loginId = "http" + index + "@example.test";
            authAccountRepository.save(AuthAccount.builder().member(learner).loginId(loginId)
                    .passwordHash(passwordEncoder.encode("http benchmark passphrase")).build());
            for (Question question : questions) {
                Answer answer = Answer.builder().member(learner).question(question)
                        .idempotencyKey(UUID.randomUUID().toString()).content("이전 학습 답변").build();
                // Historical volume is independent of the current minute's admission limit.
                ReflectionTestUtils.setField(answer, "submittedAt", LocalDateTime.now().minusDays(1));
                answer = answerRepository.save(answer);
                evaluationRepository.save(Evaluation.builder().answer(answer).build());
            }
            clients.add(login(loginId));
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("database", "PostgreSQL 17");
        report.put("transport", "loopback HTTP, authenticated session + CSRF, real embedded Tomcat");
        report.put("provider", "disabled workers; measures HTTP acceptance only");
        report.put("initialData", Map.of("questions", 100, "members", 6, "answers", 500, "evaluations", 500));
        report.put("readConcurrency", 4);
        report.put("submissionConcurrency", 5);
        Map<String, Object> results = new LinkedHashMap<>();
        for (String path : List.of("/api/questions", "/api/members/me/answers", "/api/members/me/progress")) {
            for (int warmup = 0; warmup < 5; warmup++) {
                assertThat(get(clients.getFirst().client(), path).statusCode()).isEqualTo(200);
            }
            List<Measurement> samples = new ArrayList<>();
            for (int batch = 0; batch < 10; batch++) {
                List<Callable<Measurement>> requests = new ArrayList<>();
                for (int worker = 0; worker < 4; worker++) {
                    requests.add(() -> measureGet(clients.getFirst().client(), path));
                }
                samples.addAll(ConcurrentRequests.run(requests));
            }
            results.put(path, summarize(samples, 200));
        }
        String submissionPath = "/api/questions/" + questions.getFirst().getId() + "/answers";
        for (AuthenticatedHttpClient client : clients) {
            assertThat(postAnswer(client, submissionPath).statusCode()).isEqualTo(202);
        }
        List<Measurement> submissions = new ArrayList<>();
        for (int batch = 0; batch < 5; batch++) {
            List<Callable<Measurement>> requests = new ArrayList<>();
            for (AuthenticatedHttpClient client : clients) {
                requests.add(() -> {
                    long start = System.nanoTime();
                    HttpResponse<String> response = postAnswer(client, submissionPath);
                    return new Measurement(response.statusCode(), elapsedMillis(start));
                });
            }
            submissions.addAll(ConcurrentRequests.run(requests));
        }
        results.put("answerSubmission", summarize(submissions, 202));
        report.put("results", results);
        Path output = Path.of(System.getProperty("benchmark.output"));
        Files.createDirectories(output.getParent());
        Files.writeString(output, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        assertThat(submissions).allSatisfy(sample -> assertThat(sample.status()).isEqualTo(202));
        assertThat(answerRepository.count()).isEqualTo(530);
        assertThat(evaluationRepository.count()).isEqualTo(530);
    }

    private AuthenticatedHttpClient login(String loginId) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).connectTimeout(Duration.ofSeconds(10)).build();
        JsonNode csrf = objectMapper.readTree(get(client, "/api/auth/csrf").body());
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/api/auth/login"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .header(csrf.path("headerName").stringValue(), csrf.path("token").stringValue())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                        Map.of("email", loginId, "password", "http benchmark passphrase")))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode authenticatedCsrf = objectMapper.readTree(get(client, "/api/auth/csrf").body());
        return new AuthenticatedHttpClient(client, authenticatedCsrf.path("headerName").stringValue(),
                authenticatedCsrf.path("token").stringValue());
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postAnswer(AuthenticatedHttpClient client, String path) throws Exception {
        return client.client().send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                .header(client.csrfHeader(), client.csrfToken()).header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("content", "스레드는 실행 단위입니다."))))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private Measurement measureGet(HttpClient client, String path) throws Exception {
        long start = System.nanoTime();
        HttpResponse<String> response = get(client, path);
        return new Measurement(response.statusCode(), elapsedMillis(start));
    }

    private Map<String, Object> summarize(List<Measurement> samples, int expectedStatus) {
        assertThat(samples).allSatisfy(sample -> assertThat(sample.status()).isEqualTo(expectedStatus));
        List<Double> durations = samples.stream().map(Measurement::millis).sorted().toList();
        return Map.of("samples", samples.size(), "status", expectedStatus,
                "p95Millis", durations.get((int) Math.ceil(durations.size() * 0.95) - 1),
                "maxMillis", durations.getLast());
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    private double elapsedMillis(long start) { return (System.nanoTime() - start) / 1_000_000.0; }
    private record AuthenticatedHttpClient(HttpClient client, String csrfHeader, String csrfToken) { }
    private record Measurement(int status, double millis) { }
}
