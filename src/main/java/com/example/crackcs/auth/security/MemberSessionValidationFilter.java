package com.example.crackcs.auth.security;

import com.example.crackcs.exception.MemberNotFoundException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.service.MemberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class MemberSessionValidationFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedMember principal
                && !isCurrentMemberAuthorized(principal)) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException("member session revoked"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isCurrentMemberAuthorized(AuthenticatedMember principal) {
        try {
            Member member = memberService.findById(principal.memberId());
            return member.isAuthenticatable() && member.getRole() == principal.role()
                    && member.getAuthenticationVersion() == principal.authenticationVersion();
        } catch (MemberNotFoundException missingMember) {
            return false;
        }
    }
}
