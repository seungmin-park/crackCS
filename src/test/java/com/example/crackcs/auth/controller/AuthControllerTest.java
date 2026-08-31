package com.example.crackcs.auth.controller;

import com.example.crackcs.auth.controller.request.SignUpRequest;
import com.example.crackcs.auth.service.AuthService;
import com.example.crackcs.exception.DuplicateAuthAccountException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("유효한 이메일과 비밀번호 및 닉네임으로 회원가입하면 201을 반환한다")
    void signsUpMember() throws Exception {
        Member member = member(1L, "크랙러", MemberRole.USER, MemberStatus.ACTIVE);
        SignUpRequest request = new SignUpRequest(
                "user@example.com",
                "correct horse battery staple",
                "크랙러"
        );
        given(authService.register(
                request.email(),
                request.password(),
                request.nickname()
        )).willReturn(member);

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/members/me"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nickname").value("크랙러"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

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
    @DisplayName("중복 LOCAL 이메일은 공통 409 응답으로 변환한다")
    void returnsConflictForDuplicatedEmail() throws Exception {
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_AUTH_ACCOUNT"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
    }

    private Member member(Long id, String nickname, MemberRole role, MemberStatus status) {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(id);
        given(member.getNickname()).willReturn(nickname);
        given(member.getRole()).willReturn(role);
        given(member.getStatus()).willReturn(status);
        return member;
    }
}
