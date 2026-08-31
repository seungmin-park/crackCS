package com.example.crackcs.auth.service;

import com.example.crackcs.auth.domain.AuthAccount;
import com.example.crackcs.member.domain.Member;

public interface AuthService {

    Member register(String email, String rawPassword, String nickname);

    AuthAccount recordSuccessfulLogin(String email);

}
