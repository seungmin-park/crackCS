package com.example.crackcs.auth.controller;

import com.example.crackcs.auth.controller.request.LoginRequest;
import com.example.crackcs.auth.controller.request.SignUpRequest;
import com.example.crackcs.auth.repository.AuthAccountRepository;
import com.example.crackcs.auth.repository.LoginAttemptRepository;
import com.example.crackcs.auth.repository.AuthenticationRequestBucketRepository;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"crackcs.auth.requests.login-source-limit=3",
        "crackcs.auth.requests.login-global-limit=8", "crackcs.auth.requests.sign-up-source-limit=2",
        "crackcs.auth.requests.sign-up-global-limit=3"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticationAbuseFlowTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired LoginAttemptRepository loginAttemptRepository;
    @Autowired AuthenticationRequestBucketRepository authenticationRequestBucketRepository;

    @AfterEach
    void cleanUp() {
        loginAttemptRepository.deleteAll();
        authenticationRequestBucketRepository.deleteAllInBatch();
        authAccountRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("한 주소에서 이메일을 교체해도 로그인 요청 한도를 우회하거나 실패 기록을 계속 만들 수 없다")
    void limitsLoginRequestsAcrossEmails() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/auth/login").with(csrf()).with(request -> {
                        request.setRemoteAddr("192.0.2.1"); return request;
                    }).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("unknown" + attempt + "@example.com", "wrong password"))))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").with(csrf()).with(request -> {
                    request.setRemoteAddr("192.0.2.1"); return request;
                }).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("another@example.com", "wrong password"))))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        assertThat(loginAttemptRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("실제 신규·중복 가입도 같은 공개 응답을 반환하고 기존 비밀번호와 회원을 보존한다")
    void hidesDuplicateSignUpAndPreservesOriginalAccount() throws Exception {
        SignUpRequest firstRequest = new SignUpRequest("user@example.com", "original secure passphrase", "첫 회원");
        SignUpRequest duplicateRequest = new SignUpRequest("user@example.com", "another secure passphrase", "둘째 회원");
        MvcResult firstResult = mockMvc.perform(post("/api/auth/sign-up").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isAccepted()).andReturn();

        MvcResult duplicateResult = mockMvc.perform(post("/api/auth/sign-up").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isAccepted()).andReturn();

        assertThat(duplicateResult.getResponse().getContentAsString()).isEqualTo(firstResult.getResponse().getContentAsString());
        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(authAccountRepository.count()).isEqualTo(1);
        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@example.com", firstRequest.password()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("첫 회원"));
    }

    @Test
    @DisplayName("주소와 이메일을 모두 교체해도 전체 로그인 요청 한도를 우회할 수 없다")
    void limitsLoginRequestsGlobally() throws Exception {
        for (int attempt = 0; attempt < 8; attempt++) {
            String address = "192.0.2." + (attempt + 1);
            mockMvc.perform(post("/api/auth/login").with(csrf()).with(request -> {
                        request.setRemoteAddr(address); return request;
                    }).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("unknown" + attempt + "@example.com", "wrong password"))))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").with(csrf()).with(request -> {
                    request.setRemoteAddr("198.51.100.1"); return request;
                }).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("another@example.com", "wrong password"))))
                .andExpect(status().isTooManyRequests());
        assertThat(loginAttemptRepository.count()).isEqualTo(8);
    }

    @Test
    @DisplayName("한 주소의 회원가입 한도를 넘으면 새 회원이나 인증 계정을 저장하지 않는다")
    void limitsSignUpRequestsBeforePersistence() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/auth/sign-up").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SignUpRequest("user" + attempt + "@example.com",
                            "correct horse battery staple", "회원"))))
                    .andExpect(status().is2xxSuccessful());
        }
        mockMvc.perform(post("/api/auth/sign-up").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignUpRequest("another@example.com",
                        "correct horse battery staple", "회원"))))
                .andExpect(status().isTooManyRequests());
        assertThat(memberRepository.count()).isEqualTo(2);
        assertThat(authAccountRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("주소를 교체해도 전체 회원가입 한도를 넘는 계정을 생성하지 않는다")
    void limitsSignUpRequestsGlobally() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            String address = "192.0.2." + (attempt + 1);
            mockMvc.perform(post("/api/auth/sign-up").with(csrf()).with(request -> {
                        request.setRemoteAddr(address); return request;
                    }).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SignUpRequest("user" + attempt + "@example.com",
                            "correct horse battery staple", "회원"))))
                    .andExpect(status().is2xxSuccessful());
        }
        mockMvc.perform(post("/api/auth/sign-up").with(csrf()).with(request -> {
                    request.setRemoteAddr("198.51.100.1"); return request;
                }).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignUpRequest("another@example.com",
                        "correct horse battery staple", "회원"))))
                .andExpect(status().isTooManyRequests());
        assertThat(memberRepository.count()).isEqualTo(3);
    }
}
