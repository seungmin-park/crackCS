package com.example.crackcs.auth.controller;

import com.example.crackcs.auth.controller.request.SignUpRequest;
import com.example.crackcs.auth.service.AuthService;
import com.example.crackcs.auth.service.AuthenticationRequestLimitService;
import com.example.crackcs.auth.controller.response.SignUpResponse;
import com.example.crackcs.exception.DuplicateAuthAccountException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationRequestLimitService authenticationRequestLimitService;

    @PostMapping("/sign-up")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request, HttpServletRequest httpRequest) {
        authenticationRequestLimitService.reserveSignUpRequest(httpRequest.getRemoteAddr());
        try {
            authService.register(request.email(), request.password(), request.nickname());
        } catch (DuplicateAuthAccountException duplicate) {
            // Preserve the existing account and return the same public result as a new registration.
        }
        return ResponseEntity.accepted().body(SignUpResponse.accepted());
    }
}
