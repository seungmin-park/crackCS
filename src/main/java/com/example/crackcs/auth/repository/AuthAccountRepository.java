package com.example.crackcs.auth.repository;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.domain.AuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

    boolean existsByProviderAndLoginId(AuthProvider provider, String loginId);

    @EntityGraph(attributePaths = "member")
    Optional<AuthAccount> findByProviderAndLoginId(AuthProvider provider, String loginId);
}
