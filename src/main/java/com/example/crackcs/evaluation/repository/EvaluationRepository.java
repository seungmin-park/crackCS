package com.example.crackcs.evaluation.repository;

import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByAnswerId(Long answerId);
    @Query("select e.id from Evaluation e where e.status = :status order by e.id")
    List<Long> findPendingIds(@Param("status") EvaluationStatus status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evaluation e where e.id = :id")
    Optional<Evaluation> findLockedById(@Param("id") Long id);
}
