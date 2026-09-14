package com.example.crackcs.auth.repository;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.domain.AuthProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

    boolean existsByProviderAndLoginId(AuthProvider provider, String loginId);

    @EntityGraph(attributePaths = "member")
    Optional<AuthAccount> findByProviderAndLoginId(AuthProvider provider, String loginId);
}
