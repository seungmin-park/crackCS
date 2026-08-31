package com.example.crackcs.auth.controller;

import com.example.crackcs.auth.controller.request.LoginRequest;
import com.example.crackcs.auth.controller.response.CsrfTokenResponse;
import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.service.AuthService;
import com.example.crackcs.auth.service.LoginAttemptService;
import com.example.crackcs.auth.security.AuthenticationSecurityLogger;
import com.example.crackcs.exception.InvalidCredentialsException;
import com.example.crackcs.member.controller.response.MemberResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class SessionController {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;
    private final AuthenticationSecurityLogger authenticationSecurityLogger;

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return CsrfTokenResponse.from(csrfToken);
    }

    @PostMapping("/login")
    public MemberResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String remoteAddress = httpRequest.getRemoteAddr();
        try {
            loginAttemptService.checkAllowed(request.email(), remoteAddress);
        } catch (com.example.crackcs.exception.TooManyLoginAttemptsException exception) {
            authenticationSecurityLogger.loginBlocked(request.email());
            throw exception;
        }

        Authentication authentication;
        try {
            authentication = authenticate(request);
        } catch (InvalidCredentialsException exception) {
            loginAttemptService.recordFailure(request.email(), remoteAddress);
            throw exception;
        }
        loginAttemptService.recordSuccess(request.email(), remoteAddress);
        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        AuthAccount account = authService.recordSuccessfulLogin(authentication.getName());
        return MemberResponse.from(account.getMember());
    }

    private Authentication authenticate(LoginRequest request) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email(),
                            request.password()
                    )
            );
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }
}
