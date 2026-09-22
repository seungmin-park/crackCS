package com.example.crackcs.learning.followup.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
class DefaultFollowUpCompletionTransaction implements FollowUpCompletionTransaction {

    private static final int MAX_ATTEMPTS = 3;

    private final TransactionTemplate transactionTemplate;

    @Override
    public void execute(Runnable completion) {
        for (int attempt = 1; ; attempt++) {
            try {
                transactionTemplate.executeWithoutResult(status -> completion.run());
                return;
            } catch (OptimisticLockingFailureException | PessimisticLockingFailureException conflict) {
                if (attempt == MAX_ATTEMPTS) {
                    throw conflict;
                }
            }
        }
    }
}
