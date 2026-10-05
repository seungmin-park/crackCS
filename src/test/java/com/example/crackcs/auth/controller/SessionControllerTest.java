package com.example.crackcs.auth.controller;

import com.example.crackcs.auth.controller.request.LoginRequest;
import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.auth.security.AuthenticationSecurityLogger;
import com.example.crackcs.auth.service.AuthService;
import com.example.crackcs.auth.service.LoginAttemptService;
import com.example.crackcs.exception.InvalidCredentialsException;
import com.example.crackcs.member.domain.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SessionControllerTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("로그인 최종 저장에 실패하면 세션과 현재 요청에 인증 상태를 남기지 않는다")
    void doesNotAuthenticateSessionWhenFinalLoginUpdateFails() {
        Member member = Member.builder().nickname("회원").build();
        AuthAccount account = AuthAccount.builder().member(member).loginId("user@example.com")
                .passwordHash("{bcrypt}test-only").build();
        AuthenticatedMember principal = AuthenticatedMember.from(account);
        AuthenticationManager authenticationManager = authentication ->
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        AuthService authService = mock(AuthService.class);
        when(authService.recordSuccessfulLogin("user@example.com")).thenThrow(new InvalidCredentialsException());
        SessionController controller = new SessionController(authenticationManager,
                new ChangeSessionIdAuthenticationStrategy(), new HttpSessionSecurityContextRepository(),
                authService, mock(LoginAttemptService.class), mock(AuthenticationSecurityLogger.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);

        assertThatThrownBy(() -> controller.login(new LoginRequest("user@example.com", "password"),
                request, new MockHttpServletResponse())).isInstanceOf(InvalidCredentialsException.class);

        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
