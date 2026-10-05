package com.example.crackcs.auth.controller;

import com.example.crackcs.auth.controller.request.SignUpRequest;
import com.example.crackcs.auth.service.AuthService;
import com.example.crackcs.auth.service.AuthenticationRequestLimitService;
import com.example.crackcs.exception.DuplicateAuthAccountException;
import com.example.crackcs.exception.TooManyAuthenticationRequestsException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationRequestLimitService authenticationRequestLimitService;

    @Test
    @DisplayName("신규 이메일의 회원가입 요청은 계정 정보를 노출하지 않는 202 응답을 반환한다")
    void signsUpMember() throws Exception {
        Member registeredMember = mockMember(1L, "크랙러", MemberRole.USER, MemberStatus.ACTIVE);
        SignUpRequest request = new SignUpRequest(
                "user@example.com",
                "correct horse battery staple",
                "크랙러"
        );
        given(authService.register(
                request.email(),
                request.password(),
                request.nickname()
        )).willReturn(registeredMember);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.message").value("회원가입 요청을 처리했습니다. 가입한 이메일과 비밀번호로 로그인해 주세요."))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.nickname").doesNotExist());

        verify(authService).register(
                request.email(),
                request.password(),
                request.nickname()
        );
    }

    @Test
    @DisplayName("회원가입 입력이 올바르지 않으면 DTO의 필드 오류를 반환한다")
    void rejectsInvalidSignUpRequest() throws Exception {
        SignUpRequest request = new SignUpRequest("invalid-email", "short", " ");

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'nickname')]").isNotEmpty());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("중복 이메일도 신규 이메일과 같은 202 응답과 안내 문구를 반환한다")
    void hidesDuplicatedEmailInAcceptedResponse() throws Exception {
        SignUpRequest request = new SignUpRequest(
                "user@example.com",
                "correct horse battery staple",
                "크랙러"
        );
        given(authService.register(anyString(), anyString(), anyString()))
                .willThrow(new DuplicateAuthAccountException());

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.message").value("회원가입 요청을 처리했습니다. 가입한 이메일과 비밀번호로 로그인해 주세요."))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @DisplayName("회원가입 요청 한도 초과는 계정 생성 Service 호출 전에 429로 거부된다")
    void rejectsRateLimitBeforeRegistrationWork() throws Exception {
        SignUpRequest request = new SignUpRequest("user@example.com", "correct horse battery staple", "회원");
        doThrow(new TooManyAuthenticationRequestsException(30)).when(authenticationRequestLimitService)
                .reserveSignUpRequest(anyString());

        mockMvc.perform(post("/api/auth/sign-up").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "30"));

        verifyNoInteractions(authService);
    }

    private Member mockMember(Long id, String nickname, MemberRole role, MemberStatus status) {
        Member mockedMember = mock(Member.class);
        given(mockedMember.getId()).willReturn(id);
        given(mockedMember.getNickname()).willReturn(nickname);
        given(mockedMember.getRole()).willReturn(role);
        given(mockedMember.getStatus()).willReturn(status);
        return mockedMember;
    }
}
