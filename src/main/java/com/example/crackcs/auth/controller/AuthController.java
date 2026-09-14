package com.example.crackcs.auth.controller;

import com.example.crackcs.auth.controller.request.SignUpRequest;
import com.example.crackcs.auth.service.AuthService;
import com.example.crackcs.member.controller.response.MemberResponse;
import com.example.crackcs.member.domain.Member;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Member member = authService.register(
                request.email(),
                request.password(),
                request.nickname()
        );
        return ResponseEntity
                .created(URI.create("/api/members/me"))
                .body(MemberResponse.from(member));
    }
}
