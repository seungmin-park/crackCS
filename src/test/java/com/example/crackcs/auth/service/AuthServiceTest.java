package com.example.crackcs.auth.service;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.domain.AuthProvider;
import com.example.crackcs.auth.repository.AuthAccountRepository;
import com.example.crackcs.exception.DuplicateAuthAccountException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(AuthServiceTest.FailingPasswordEncoderConfiguration.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "correct horse battery staple";

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthAccountRepository authAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        authAccountRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원과 LOCAL 인증 계정을 한 트랜잭션에서 생성하고 비밀번호를 해시한다")
    void registersMemberAndHashedLocalAccount() {
        Member member = authService.register("USER@Example.com", RAW_PASSWORD, "크랙러");

        AuthAccount account = authAccountRepository.findByProviderAndLoginId(
                        AuthProvider.LOCAL,
                        "user@example.com"
                )
                .orElseThrow();
        assertThat(account.getMemberId()).isEqualTo(member.getId());
        assertThat(account.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, account.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("중복 LOCAL 이메일 가입을 거부하고 추가 회원을 만들지 않는다")
    void rejectsDuplicatedLocalEmail() {
        authService.register("user@example.com", RAW_PASSWORD, "첫 회원");

        assertThatThrownBy(() -> authService.register(
                "USER@example.com",
                "another secure passphrase",
                "둘째 회원"
        )).isInstanceOf(DuplicateAuthAccountException.class);
        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(authAccountRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("비밀번호 해시에 실패하면 먼저 저장한 회원도 롤백한다")
    void rollsBackMemberWhenPasswordEncodingFails() {
        assertThatThrownBy(() -> authService.register(
                "rollback@example.com",
                FailingPasswordEncoderConfiguration.FAILING_PASSWORD,
                "롤백 회원"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("forced password encoding failure");
        assertThat(memberRepository.count()).isZero();
        assertThat(authAccountRepository.count()).isZero();
    }

    @TestConfiguration
    static class FailingPasswordEncoderConfiguration {

        private static final String FAILING_PASSWORD = "force password encoding failure";

        @Bean
        @Primary
        PasswordEncoder failingPasswordEncoder() {
            PasswordEncoder delegate = PasswordEncoderFactories.createDelegatingPasswordEncoder();
            return new PasswordEncoder() {
                @Override
                public String encode(CharSequence rawPassword) {
                    if (FAILING_PASSWORD.contentEquals(rawPassword)) {
                        throw new IllegalStateException("forced password encoding failure");
                    }
                    return delegate.encode(rawPassword);
                }

                @Override
                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return delegate.matches(rawPassword, encodedPassword);
                }
            };
        }
    }
}
