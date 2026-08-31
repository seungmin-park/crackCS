package com.example.crackcs.auth.repository;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.domain.AuthProvider;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AuthAccountRepositoryTest {

    @Autowired
    private AuthAccountRepository authAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("LOCAL 인증 계정을 회원과 함께 저장하고 이메일로 조회한다")
    void savesAndFindsLocalAccount() {
        Member member = memberRepository.save(Member.builder()
                .nickname("크랙러")
                .build());
        authAccountRepository.save(AuthAccount.builder()
                .member(member)
                .loginId("user@example.com")
                .passwordHash("{bcrypt}encoded-password")
                .build());

        AuthAccount found = authAccountRepository.findByProviderAndLoginId(
                        AuthProvider.LOCAL,
                        "user@example.com"
                )
                .orElseThrow();

        assertThat(found.getMemberId()).isEqualTo(member.getId());
        assertThat(found.getMember().getNickname()).isEqualTo("크랙러");
        assertThat(found.getPasswordHash()).isEqualTo("{bcrypt}encoded-password");
    }

    @Test
    @DisplayName("같은 provider와 이메일의 인증 계정을 중복 저장할 수 없다")
    void rejectsDuplicatedProviderAndLoginId() {
        Member firstMember = memberRepository.save(Member.builder().nickname("첫 회원").build());
        Member secondMember = memberRepository.save(Member.builder().nickname("둘째 회원").build());
        authAccountRepository.save(AuthAccount.builder()
                .member(firstMember)
                .loginId("user@example.com")
                .passwordHash("{bcrypt}first")
                .build());
        AuthAccount duplicated = AuthAccount.builder()
                .member(secondMember)
                .loginId("user@example.com")
                .passwordHash("{bcrypt}second")
                .build();

        assertThatThrownBy(() -> authAccountRepository.save(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
