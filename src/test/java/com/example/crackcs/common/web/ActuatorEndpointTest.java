package com.example.crackcs.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.web.exposure.include=health,prometheus",
                "management.endpoint.health.probes.enabled=true",
                "crackcs.evaluation.worker-enabled=false"
        }
)
class ActuatorEndpointTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalManagementPort
    private int managementPort;

    @Test
    @DisplayName("관리 포트에서 애플리케이션 상태를 확인한다")
    void exposesHealthOnManagementPort() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("관리 포트에서 liveness와 readiness 상태를 확인한다")
    void exposesHealthProbesOnManagementPort() throws Exception {
        HttpResponse<String> liveness = get("/actuator/health/liveness");
        HttpResponse<String> readiness = get("/actuator/health/readiness");

        assertThat(liveness.statusCode()).isEqualTo(200);
        assertThat(readiness.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("관리 포트에서 Prometheus 수집 형식의 메트릭을 제공한다")
    void exposesPrometheusMetricsOnManagementPort() throws Exception {
        HttpResponse<String> response = get("/actuator/prometheus");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("# HELP", "# TYPE");
    }

    @Test
    @DisplayName("허용하지 않은 Actuator 엔드포인트는 노출하지 않는다")
    void hidesUnexposedActuatorEndpoint() throws Exception {
        HttpResponse<String> response = get("/actuator/beans");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + managementPort + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
