package com.example.crackcs.learning.followup.service;

/**
 * Runs follow-up completion in a fresh transaction and retries transient storage conflicts.
 */
public interface FollowUpCompletionTransaction {
    void execute(Runnable completion);
}
