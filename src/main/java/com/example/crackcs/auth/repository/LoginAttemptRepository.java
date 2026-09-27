package com.example.crackcs.auth.repository;

import com.example.crackcs.auth.domain.LoginAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    Optional<LoginAttempt> findByAttemptKey(String attemptKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from LoginAttempt attempt where attempt.attemptKey = :attemptKey")
    Optional<LoginAttempt> findByAttemptKeyForUpdate(@Param("attemptKey") String attemptKey);

    @Query("""
            select attempt.attemptKey from LoginAttempt attempt
            where (attempt.blockedUntil is null and attempt.updatedAt < :threshold)
               or attempt.blockedUntil <= :now
            order by attempt.updatedAt, attempt.id
            """)
    List<String> findExpiredKeys(@Param("threshold") Instant threshold, @Param("now") Instant now, Pageable pageable);
}
