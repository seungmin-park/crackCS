package com.example.crackcs.learning.answer.repository;

import java.time.LocalDateTime;

public interface LastAnsweredQuestion {
    Long getQuestionId();

    LocalDateTime getLastAnsweredAt();
}
