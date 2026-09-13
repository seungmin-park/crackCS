package com.example.crackcs.evaluation.service;

/**
 * Runs evaluation completion and knowledge application atomically in a fresh transaction.
 */
public interface EvaluationCompletionTransaction {
    void execute(Runnable completion);
}
