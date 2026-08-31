package com.example.crackcs.auth.security;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.auth.domain.AuthProvider;
import com.example.crackcs.auth.repository.AuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultMemberUserDetailsService implements UserDetailsService {

    private final AuthAccountRepository authAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthAccount account = authAccountRepository.findByProviderAndLoginId(
                        AuthProvider.LOCAL,
                        AuthAccount.normalizeLoginId(username)
                )
                .orElseThrow(() -> new UsernameNotFoundException("authentication failed"));
        return AuthenticatedMember.from(account);
    }
}
