package com.example.crackcs.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("비로그인 사용자의 문제 API 요청을 공통 401 응답으로 거부한다")
    void rejectsAnonymousQuestionRequest() throws Exception {
        String requestId = "f0522cf1-3071-444e-bcfd-a2b2b510315a";

        mockMvc.perform(get("/api/questions").header("X-Request-Id", requestId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    @DisplayName("기존 업무 API health 경로는 더 이상 공개하지 않는다")
    void rejectsLegacyHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("USER의 관리자 API 요청을 공통 403 응답으로 거부한다")
    void rejectsUserRequestToAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/questions")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/topics",
            "/api/admin/concepts",
            "/api/admin/knowledge-documents",
            "/api/admin/questions",
            "/api/admin/members",
            "/api/admin/evaluations"
    })
    @DisplayName("USER는 모든 관리자 API 계열에서 서버 인가로 차단된다")
    void rejectsUserFromEveryAdminApiFamily(String path) throws Exception {
        mockMvc.perform(get(path).with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("USER는 답변 원문이 포함될 수 있는 관리자 평가 API에 접근할 수 없다")
    void rejectsUserRequestToAdminEvaluationApi() throws Exception {
        mockMvc.perform(get("/api/admin/evaluations/1")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("ADMIN은 관리자 API에 접근할 수 있다")
    void permitsAdminRequestToAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/questions")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PasswordEncoder는 원문이 아닌 단방향 해시를 생성한다")
    void encodesPasswordWithAdaptiveOneWayHash() {
        String rawPassword = "correct horse battery staple";

        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
    }
}
