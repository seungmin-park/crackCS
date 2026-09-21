package com.example.crackcs.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "crackcs.evaluation.worker-enabled=false")
@AutoConfigureMockMvc
@Import(RequestIdFilterTest.FailureController.class)
class RequestIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("인증 실패 응답은 요청 헤더와 오류 본문에 같은 요청 ID를 사용한다")
    void correlatesSecurityErrorWithRequestId() throws Exception {
        String requestId = "a780b07a-491f-4149-88c6-a0fe12978f4c";

        mockMvc.perform(get("/api/questions").header("X-Request-Id", requestId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    @DisplayName("유효하지 않은 요청 ID는 새 UUID로 교체한다")
    void replacesInvalidRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/questions").header("X-Request-Id", "not-a-uuid"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        String responseRequestId = result.getResponse().getHeader("X-Request-Id");
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(responseRequestId).isNotEqualTo("not-a-uuid");
        assertThat(UUID.fromString(responseRequestId).toString()).isEqualTo(responseRequestId);
        assertThat(body.get("requestId").asText()).isEqualTo(responseRequestId);
    }

    @Test
    @DisplayName("Controller 예외 응답도 요청 헤더와 오류 본문에 같은 요청 ID를 사용한다")
    void correlatesControllerErrorWithRequestId() throws Exception {
        String requestId = "e7499bab-7363-4cf6-a17c-11b17d89c20b";

        mockMvc.perform(get("/api/members/me/request-id-failure")
                        .with(user("user@example.com").roles("USER"))
                        .header("X-Request-Id", requestId))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @RestController
    static class FailureController {

        @GetMapping("/api/members/me/request-id-failure")
        void fail() {
            throw new RuntimeException("sensitive-internal-detail");
        }
    }
}
