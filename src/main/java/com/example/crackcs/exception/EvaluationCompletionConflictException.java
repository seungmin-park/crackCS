package com.example.crackcs.exception;

import org.springframework.dao.ConcurrencyFailureException;

/**
 * An insert-if-absent race still conflicts after the completion transaction retry limit.
 */
public class EvaluationCompletionConflictException extends ConcurrencyFailureException {
    public EvaluationCompletionConflictException(Throwable cause) {
        super("evaluation completion conflicts persisted after transaction retries", cause);
    }
}
