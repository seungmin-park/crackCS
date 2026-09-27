package com.example.crackcs.learning.answer.service;

import com.example.crackcs.exception.TooManyAnswerRequestsException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Component
public final class AnswerSubmissionPolicy {
    private final int maxSubmissionsPerMinute;

    public AnswerSubmissionPolicy(@Value("${crackcs.answer.max-submissions-per-minute:10}") int maxSubmissionsPerMinute) {
        if (maxSubmissionsPerMinute < 1) {
            throw new IllegalArgumentException("maxSubmissionsPerMinute must be positive");
        }
        this.maxSubmissionsPerMinute = maxSubmissionsPerMinute;
    }

    public LocalDateTime windowStart(LocalDateTime now) {
        return now.minusMinutes(1);
    }

    public void verifyAllowed(long recentSubmissionCount) {
        if (recentSubmissionCount >= maxSubmissionsPerMinute) {
            throw new TooManyAnswerRequestsException();
        }
    }
}
