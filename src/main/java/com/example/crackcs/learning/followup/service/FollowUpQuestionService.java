package com.example.crackcs.learning.followup.service;

public interface FollowUpQuestionService {
    FollowUpQuestionResult findByAnswerId(Long memberId, Long answerId);
}
