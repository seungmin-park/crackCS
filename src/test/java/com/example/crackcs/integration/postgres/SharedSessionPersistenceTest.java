package com.example.crackcs.integration.postgres;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.security.AuthenticatedMember;
import com.example.crackcs.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("postgres")
@SpringBootTest
class SharedSessionPersistenceTest {
    @Autowired ApplicationContext context;
    @Autowired DataSource dataSource;

    @Test
    @DisplayName("다른 서버가 로그인과 CSRF를 복원하고 로그아웃을 함께 반영한다")
    void independentRepositoryReadsAuthenticationAndCsrfAndObservesLogout() {
        assertThat(context.getBeansOfType(SessionRepository.class)).hasSize(1);
        SessionRepository<Session> first = repository(context.getBean(SessionRepository.class));
        SessionRepository<Session> second = independentRepository();
        AuthAccount account = AuthAccount.builder().member(Member.builder().nickname("공유 세션").build())
                .loginId("shared@example.com").passwordHash("{bcrypt}test-only").build();
        AuthenticatedMember principal = AuthenticatedMember.from(account);
        Session session = first.createSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())));
        session.setAttribute("csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "shared-token"));
        first.save(session);

        Session restored = second.findById(session.getId());
        assertThat(restored).isNotNull();
        SecurityContextImpl security = restored.getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(security.getAuthentication().getPrincipal()).isInstanceOf(AuthenticatedMember.class);
        assertThat(security.getAuthentication().getName()).isEqualTo("shared@example.com");
        DefaultCsrfToken csrf = restored.getAttribute("csrf");
        assertThat(csrf.getToken()).isEqualTo("shared-token");
        second.deleteById(session.getId());
        assertThat(first.findById(session.getId())).isNull();
    }

    @Test
    @DisplayName("다른 서버에서도 만료된 세션을 복원할 수 없다")
    void expiredSessionCannotBeRestoredOnAnotherInstance() {
        assertThat(context.getBeansOfType(SessionRepository.class)).hasSize(1);
        SessionRepository<Session> first = repository(context.getBean(SessionRepository.class));
        Session session = first.createSession();
        session.setMaxInactiveInterval(Duration.ofMinutes(30));
        session.setLastAccessedTime(Instant.now().minusSeconds(1801));
        first.save(session);
        assertThat(independentRepository().findById(session.getId())).isNull();
    }

    private SessionRepository<Session> independentRepository() {
        return repository(new JdbcIndexedSessionRepository(new JdbcTemplate(dataSource),
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))));
    }

    @SuppressWarnings("unchecked")
    private SessionRepository<Session> repository(SessionRepository<?> repository) {
        return (SessionRepository<Session>) repository;
    }
}
