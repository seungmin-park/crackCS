package com.example.crackcs.auth.service;

public interface AuthenticationRequestLimitService {
    void reserveLoginRequest(String remoteAddress);
    void reserveSignUpRequest(String remoteAddress);
}
