package com.example.crackcs.learning.followup.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class DefaultFollowUpCompletionTransactionTest {

    @Test
    @DisplayName("일시적 저장 충돌은 새 트랜잭션에서 완료 작업을 다시 시도한다")
    void retriesTransientStorageConflict() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        AtomicInteger transactionAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (transactionAttempts.incrementAndGet() < 3) {
                throw new PessimisticLockingFailureException("temporary conflict");
            }
            invocation.<java.util.function.Consumer<?>>getArgument(0).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        DefaultFollowUpCompletionTransaction completionTransaction =
                new DefaultFollowUpCompletionTransaction(transactionTemplate);
        AtomicInteger completions = new AtomicInteger();

        completionTransaction.execute(completions::incrementAndGet);

        assertThat(transactionAttempts).hasValue(3);
        assertThat(completions).hasValue(1);
    }

    @Test
    @DisplayName("세 번째 저장 충돌은 완료 작업을 적용하지 않고 호출자에게 전달한다")
    void propagatesExhaustedStorageConflict() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        AtomicInteger transactionAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            transactionAttempts.incrementAndGet();
            throw new PessimisticLockingFailureException("persistent conflict");
        }).when(transactionTemplate).executeWithoutResult(any());
        DefaultFollowUpCompletionTransaction completionTransaction =
                new DefaultFollowUpCompletionTransaction(transactionTemplate);
        AtomicInteger completions = new AtomicInteger();

        assertThatThrownBy(() -> completionTransaction.execute(completions::incrementAndGet))
                .isInstanceOf(PessimisticLockingFailureException.class);
        assertThat(transactionAttempts).hasValue(3);
        assertThat(completions).hasValue(0);
    }
}
