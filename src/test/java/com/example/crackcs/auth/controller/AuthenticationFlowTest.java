package com.example.crackcs.auth.controller;

import com.example.crackcs.auth.controller.request.LoginRequest;
import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.domain.AuthProvider;
import com.example.crackcs.auth.repository.AuthAccountRepository;
import com.example.crackcs.auth.repository.LoginAttemptRepository;
import com.example.crackcs.auth.repository.AuthenticationRequestBucketRepository;
import com.example.crackcs.auth.service.AuthService;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberStatus;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.controller.request.MemberStatusUpdateRequest;
import com.example.crackcs.member.service.MemberService;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AuthenticationFlowTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthAccountRepository authAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberService memberService;

    @Autowired
    private AuthenticationRequestBucketRepository authenticationRequestBucketRepository;

    @AfterEach
    void cleanUp() {
        loginAttemptRepository.deleteAll();
        authenticationRequestBucketRepository.deleteAllInBatch();
        authAccountRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("로그인하면 세션에 인증 상태를 저장하고 현재 회원과 로그인 시각을 조회한다")
    void logsInAndRestoresCurrentMemberFromSession() throws Exception {
        authService.register("user@example.com", PASSWORD, "크랙러");
        CsrfFixture csrf = issueCsrfToken();
        String anonymousSessionId = csrf.session().getId();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("user@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("크랙러"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andExpect(jsonPath("$.csrfToken").doesNotExist())
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andReturn();
        MockHttpSession authenticatedSession = sessionOf(loginResult);

        assertThat(authenticatedSession.getId()).isNotEqualTo(anonymousSessionId);
        mockMvc.perform(get("/api/members/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("크랙러"))
                .andExpect(jsonPath("$.role").value("USER"));
        AuthAccount authenticatedAccount = authAccountRepository.findByProviderAndLoginId(
                        AuthProvider.LOCAL,
                        "user@example.com"
                )
                .orElseThrow();
        assertThat(authenticatedAccount.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 계정과 틀린 비밀번호는 같은 인증 실패 응답을 반환한다")
    void hidesWhetherLoginAccountExists() throws Exception {
        authService.register("user@example.com", PASSWORD, "크랙러");
        CsrfFixture wrongPasswordCsrf = issueCsrfToken();
        CsrfFixture unknownAccountCsrf = issueCsrfToken();

        mockMvc.perform(post("/api/auth/login")
                        .session(wrongPasswordCsrf.session())
                        .header(wrongPasswordCsrf.headerName(), wrongPasswordCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("user@example.com", "this password is incorrect")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        mockMvc.perform(post("/api/auth/login")
                        .session(unknownAccountCsrf.session())
                        .header(unknownAccountCsrf.headerName(), unknownAccountCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown@example.com", "this password is incorrect")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @ParameterizedTest
    @EnumSource(value = MemberStatus.class, names = {"BLOCKED", "WITHDRAWN"})
    @DisplayName("비활성 회원은 올바른 비밀번호로도 로그인할 수 없다")
    void rejectsInactiveMemberLogin(MemberStatus memberStatus) throws Exception {
        String email = memberStatus.name().toLowerCase() + "@example.com";
        Member inactiveMember = authService.register(email, PASSWORD, "비활성 회원");
        inactiveMember.changeStatus(memberStatus);
        memberRepository.save(inactiveMember);
        CsrfFixture csrf = issueCsrfToken();

        mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("로그아웃하면 기존 세션으로 현재 회원을 조회할 수 없다")
    void invalidatesAuthenticationOnLogout() throws Exception {
        authService.register("user@example.com", PASSWORD, "크랙러");
        CsrfFixture csrf = issueCsrfToken();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("user@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession authenticatedSession = sessionOf(loginResult);

        mockMvc.perform(post("/api/auth/logout")
                        .session(authenticatedSession)
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/me").session(authenticatedSession))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/topics", "/api/admin/concepts", "/api/admin/knowledge-documents",
            "/api/admin/questions", "/api/admin/members", "/api/admin/evaluations"})
    @DisplayName("로그인한 관리자가 차단되면 기존 세션으로 모든 관리자 API 계열에 접근할 수 없다")
    void rejectsBlockedAdministratorSession(String path) throws Exception {
        Member administrator = memberRepository.save(Member.builder()
                .nickname("관리자").role(MemberRole.ADMIN).build());
        authAccountRepository.save(AuthAccount.builder().member(administrator)
                .loginId("admin@example.com").passwordHash(passwordEncoder.encode(PASSWORD)).build());
        CsrfFixture csrf = issueCsrfToken();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header(csrf.headerName(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin@example.com", PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession authenticatedSession = sessionOf(loginResult);
        mockMvc.perform(get("/api/admin/members").session(authenticatedSession)).andExpect(status().isOk());

        memberService.changeStatus(administrator.getId(), MemberStatus.BLOCKED);

        mockMvc.perform(get(path).session(authenticatedSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        assertThat(authenticatedSession.isInvalid()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = MemberStatus.class, names = {"BLOCKED", "WITHDRAWN"})
    @DisplayName("차단 또는 탈퇴한 관리자는 기존 세션으로 자신을 다시 활성화할 수 없다")
    void rejectsInactiveAdministratorSelfReactivation(MemberStatus status) throws Exception {
        Member administrator = memberRepository.save(Member.builder()
                .nickname("관리자").role(MemberRole.ADMIN).build());
        authAccountRepository.save(AuthAccount.builder().member(administrator)
                .loginId("admin@example.com").passwordHash(passwordEncoder.encode(PASSWORD)).build());
        CsrfFixture csrf = issueCsrfToken();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header(csrf.headerName(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin@example.com", PASSWORD)))
                .andExpect(status().isOk()).andReturn();

        memberService.changeStatus(administrator.getId(), status);

        mockMvc.perform(patch("/api/admin/members/{memberId}/status", administrator.getId())
                        .session(sessionOf(loginResult)).header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberStatusUpdateRequest(MemberStatus.ACTIVE))))
                .andExpect(status().isUnauthorized());
        assertThat(memberRepository.findById(administrator.getId()).orElseThrow().getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = MemberStatus.class, names = {"BLOCKED", "WITHDRAWN"})
    @DisplayName("일반 회원도 차단 또는 탈퇴 후 기존 세션으로 현재 회원을 조회할 수 없다")
    void rejectsInactiveUserSession(MemberStatus status) throws Exception {
        Member member = authService.register("user@example.com", PASSWORD, "회원");
        CsrfFixture csrf = issueCsrfToken();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header(csrf.headerName(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("user@example.com", PASSWORD)))
                .andExpect(status().isOk()).andReturn();

        memberService.changeStatus(member.getId(), status);

        mockMvc.perform(get("/api/members/me").session(sessionOf(loginResult)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("차단 후 다시 활성화해도 차단 전에 발급한 세션은 재사용할 수 없다")
    void rejectsOldSessionAfterMemberReactivation() throws Exception {
        Member member = authService.register("user@example.com", PASSWORD, "회원");
        CsrfFixture csrf = issueCsrfToken();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").session(csrf.session())
                        .header(csrf.headerName(), csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("user@example.com", PASSWORD)))
                .andExpect(status().isOk()).andReturn();

        memberService.changeStatus(member.getId(), MemberStatus.BLOCKED);
        memberService.changeStatus(member.getId(), MemberStatus.ACTIVE);

        mockMvc.perform(get("/api/members/me").session(sessionOf(loginResult)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-006 USER 권한 세션으로 관리자 API에 접근하면 서버가 거부한다")
    void rejectsUserSessionFromAdminApi() throws Exception {
        authService.register("user@example.com", PASSWORD, "일반 회원");
        CsrfFixture csrf = issueCsrfToken();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("user@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/admin/questions").session(sessionOf(loginResult)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("로그인 실패 제한은 429를 반환하고 로그에는 마스킹 이메일만 남긴다")
    void limitsRepeatedLoginFailuresWithoutLeakingCredentials(CapturedOutput output) throws Exception {
        authService.register("rate@example.com", PASSWORD, "제한 회원");
        CsrfFixture csrf = issueCsrfToken();
        String wrongPassword = "this password must never appear in logs";

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .session(csrf.session())
                            .header(csrf.headerName(), csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("rate@example.com", wrongPassword)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("rate@example.com", wrongPassword)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));

        assertThat(output).contains("r***@example.com");
        assertThat(output).doesNotContain("rate@example.com");
        assertThat(output).doesNotContain(wrongPassword);
        assertThat(output).doesNotContain(csrf.token());
    }

    @Test
    @DisplayName("CSRF token이 없는 로그인 요청은 인증 처리 전에 거부한다")
    void rejectsLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("user@example.com", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("허용하지 않은 origin의 CORS preflight에는 credential 허용 header를 제공하지 않는다")
    void rejectsCrossOriginCredentialRequest() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://attacker.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(
                        result.getResponse().getHeader("Access-Control-Allow-Origin")
                ).isNull())
                .andExpect(result -> assertThat(
                        result.getResponse().getHeader("Access-Control-Allow-Credentials")
                ).isNull());
    }

    private CsrfFixture issueCsrfToken() throws Exception {
        MvcResult csrfResponse = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode responseBody = objectMapper.readTree(csrfResponse.getResponse().getContentAsString());
        return new CsrfFixture(
                sessionOf(csrfResponse),
                responseBody.get("headerName").asText(),
                responseBody.get("token").asText()
        );
    }

    private MockHttpSession sessionOf(MvcResult mvcResult) {
        return (MockHttpSession) mvcResult.getRequest().getSession(false);
    }

    private String loginJson(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginRequest(email, password));
    }

    private record CsrfFixture(MockHttpSession session, String headerName, String token) {
    }
}
