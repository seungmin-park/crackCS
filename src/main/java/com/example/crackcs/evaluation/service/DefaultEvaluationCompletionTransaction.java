package com.example.crackcs.evaluation.service;

import com.example.crackcs.exception.EvaluationCompletionConflictException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DefaultEvaluationCompletionTransaction implements EvaluationCompletionTransaction {
    private static final int MAX_ATTEMPTS = 8;
    private static final Set<String> KNOWLEDGE_CONSTRAINTS = Set.of(
            "uk_knowledge_state_member_concept", "uk_knowledge_application_evaluation_concept");
    private final TransactionTemplate transactions;

    public DefaultEvaluationCompletionTransaction(PlatformTransactionManager manager) {
        transactions = new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void execute(Runnable completion) {
        for (int attempt = 1; ; attempt++) {
            try {
                transactions.executeWithoutResult(status -> completion.run());
                return;
            } catch (OptimisticLockingFailureException | PessimisticLockingFailureException conflict) {
                if (attempt == MAX_ATTEMPTS) {
                    throw conflict;
                }
            } catch (DataIntegrityViolationException violation) {
                if (!isKnowledgeUniqueConflict(violation)) {
                    throw violation;
                }
                if (attempt == MAX_ATTEMPTS) {
                    throw new EvaluationCompletionConflictException(violation);
                }
            }
        }
    }

    private boolean isKnowledgeUniqueConflict(DataIntegrityViolationException violation) {
        for (Throwable cause = violation; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraint) {
                SQLException sql = constraint.getSQLException();
                String name = constraint.getConstraintName();
                if (!"23505".equals(sql.getSQLState()) || name == null) {
                    return false;
                }
                String driverException = sql.getClass().getName();
                if (driverException.equals("org.postgresql.util.PSQLException")) {
                    return KNOWLEDGE_CONSTRAINTS.contains(name);
                }
                if (driverException.equals("org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException")
                        && sql.getErrorCode() == 23505) {
                    // H2 2.4 reports the named constraint followed by its generated backing index.
                    return KNOWLEDGE_CONSTRAINTS.stream().anyMatch(known -> {
                        String qualified = "PUBLIC." + known.toUpperCase(Locale.ROOT);
                        return name.equals(qualified) || name.matches(
                                qualified.replace(".", "\\.") + " INDEX "
                                        + qualified.replace(".", "\\.") + "_INDEX_[0-9A-F]+");
                    });
                }
                return false;
            }
        }
        return false;
    }
}
