package com.example.crackcs.auth.security;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

public class AuthenticatedMember implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long memberId;
    private final String loginId;
    private final String passwordHash;
    private final MemberRole role;
    private final MemberStatus status;

    private AuthenticatedMember(Long memberId, String loginId, String passwordHash, MemberRole role, MemberStatus status) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public static AuthenticatedMember from(AuthAccount account) {
        return new AuthenticatedMember(
                account.getMemberId(),
                account.getLoginId(),
                account.getPasswordHash(),
                account.getMember().getRole(),
                account.getMember().getStatus());
    }

    public Long memberId() {
        return memberId;
    }

    public MemberRole role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isEnabled() {
        return status == MemberStatus.ACTIVE;
    }
}
