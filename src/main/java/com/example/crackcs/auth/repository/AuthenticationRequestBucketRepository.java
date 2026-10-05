package com.example.crackcs.auth.repository;

import com.example.crackcs.auth.domain.AuthenticationRequestBucket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuthenticationRequestBucketRepository extends JpaRepository<AuthenticationRequestBucket, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bucket from AuthenticationRequestBucket bucket where bucket.bucketKey = :key")
    Optional<AuthenticationRequestBucket> findByBucketKeyForUpdate(@Param("key") String key);

    @Modifying
    @Query("delete from AuthenticationRequestBucket bucket where bucket.windowStartedAt < :cutoff and bucket.bucketKey not in :guardKeys")
    int deleteExpiredSources(@Param("cutoff") Instant cutoff, @Param("guardKeys") List<String> guardKeys);
}
