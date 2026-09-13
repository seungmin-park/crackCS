package com.example.crackcs.evaluation.repository;

import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long>, EvaluationDetailsRepository {
    @EntityGraph(attributePaths = {
            "concepts", "concepts.concept", "evidence", "evidence.chunk", "evidence.chunk.document"
    })
    Optional<Evaluation> findByAnswerId(Long answerId);

    @Query("""
            select e.id from Evaluation e
            where (e.status = :evaluating
                    and e.nextAttemptAt <= :now)
               or (e.status = :processing
                    and e.leaseExpiresAt < :now)
            order by e.createdAt, e.id
            """)
    List<Long> findClaimableIds(@Param("now") LocalDateTime now,
                                @Param("evaluating") EvaluationStatus evaluating,
                                @Param("processing") EvaluationStatus processing, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evaluation e where e.id = :id")
    Optional<Evaluation> findLockedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"answer", "answer.question"})
    Page<Evaluation> findByStatusIn(List<EvaluationStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"answer", "answer.question", "evidence", "evidence.chunk",
            "evidence.chunk.document"})
    @Query("select e from Evaluation e where e.id = :id")
    Optional<Evaluation> findAdminDetailById(@Param("id") Long id);

    @Query("select coalesce(sum(e.inputTokens), 0) from Evaluation e where e.evaluatedAt >= :from and e.evaluatedAt < :until")
    long sumInputTokensBetween(@Param("from") LocalDateTime from,
                               @Param("until") LocalDateTime until);

    @Query("select coalesce(sum(e.outputTokens), 0) from Evaluation e where e.evaluatedAt >= :from and e.evaluatedAt < :until")
    long sumOutputTokensBetween(@Param("from") LocalDateTime from,
                                @Param("until") LocalDateTime until);
}
