package com.example.crackcs.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "crackcs.security.allowed-hosts=localhost,127.0.0.1")
@AutoConfigureMockMvc
class AllowedHostFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("허용 호스트 설정이 있으면 DNS 리바인딩 호스트의 CSRF 토큰 요청을 거부한다")
    void rejectsCsrfRequestFromRebindingHost() throws Exception {
        mockMvc.perform(get("http://rebind.example/api/auth/csrf"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("허용 호스트 설정이 있어도 loopback 호스트의 CSRF 토큰 요청은 처리한다")
    void servesCsrfRequestFromLoopbackHost() throws Exception {
        mockMvc.perform(get("http://127.0.0.1/api/auth/csrf"))
                .andExpect(status().isOk());
    }
}
