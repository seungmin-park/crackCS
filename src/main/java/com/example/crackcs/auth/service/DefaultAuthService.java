package com.example.crackcs.auth.service;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.domain.AuthProvider;
import com.example.crackcs.auth.repository.AuthAccountRepository;
import com.example.crackcs.exception.DuplicateAuthAccountException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAuthService implements AuthService {

    private final MemberRepository memberRepository;
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Member register(String email, String rawPassword, String nickname) {
        String loginId = AuthAccount.normalizeLoginId(email);
        if (authAccountRepository.existsByProviderAndLoginId(AuthProvider.LOCAL, loginId)) {
            throw new DuplicateAuthAccountException();
        }

        Member member = memberRepository.save(Member.builder()
                .nickname(nickname)
                .build());
        String passwordHash = passwordEncoder.encode(rawPassword);
        AuthAccount account = AuthAccount.builder()
                .member(member)
                .loginId(loginId)
                .passwordHash(passwordHash)
                .build();

        try {
            authAccountRepository.save(account);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateAuthAccountException();
        }
        return member;
    }

    @Override
    @Transactional
    public AuthAccount recordSuccessfulLogin(String email) {
        AuthAccount account = findAccount(email);
        account.recordSuccessfulLogin();
        return account;
    }

    private AuthAccount findAccount(String email) {
        return authAccountRepository.findByProviderAndLoginId(
                        AuthProvider.LOCAL,
                        AuthAccount.normalizeLoginId(email)
                )
                .orElseThrow(() -> new IllegalArgumentException("authentication failed"));
    }
}
