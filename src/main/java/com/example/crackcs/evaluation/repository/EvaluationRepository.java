package com.example.crackcs.evaluation.repository;

import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    @EntityGraph(attributePaths = {
            "concepts", "concepts.concept", "evidence", "evidence.chunk", "evidence.chunk.document"
    })
    Optional<Evaluation> findByAnswerId(Long answerId);

    @Query("select distinct e from Evaluation e left join fetch e.concepts c left join fetch c.concept where e.answer.id in :answerIds")
    List<Evaluation> findAllWithConceptsByAnswerIdIn(@Param("answerIds") List<Long> answerIds);

    @Query("""
            select e.id from Evaluation e
            where (e.status = com.example.crackcs.evaluation.domain.EvaluationStatus.EVALUATING
                    and e.nextAttemptAt <= :now)
               or (e.status = com.example.crackcs.evaluation.domain.EvaluationStatus.PROCESSING
                    and e.leaseExpiresAt < :now)
            order by e.createdAt, e.id
            """)
    List<Long> findClaimableIds(@Param("now") LocalDateTime now, Pageable pageable);

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
