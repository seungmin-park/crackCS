package com.example.crackcs.evaluation.domain;

import lombok.Getter;

@Getter
public enum Verdict {
    CORRECT(100), PARTIALLY_CORRECT(50), INCORRECT(0), NEEDS_REVIEW(null);

    private final Integer score;

    Verdict(Integer score) {
        this.score = score;
    }
}
