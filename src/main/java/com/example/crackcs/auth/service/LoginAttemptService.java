package com.example.crackcs.auth.service;

public interface LoginAttemptService {

    void checkAllowed(String loginId, String remoteAddress);

    void recordFailure(String loginId, String remoteAddress);

    void recordSuccess(String loginId, String remoteAddress);
}
