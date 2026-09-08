package com.example.crackcs.member.repository;

import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select member from Member member where member.id = :id")
    Optional<Member> findLockedById(@Param("id") Long id);

    @Query("""
            SELECT member
            FROM Member member
            WHERE (:role IS NULL OR member.role = :role)
              AND (:status IS NULL OR member.status = :status)
            """)
    Page<Member> findAllByConditions(
            @Param("role") MemberRole role,
            @Param("status") MemberStatus status,
            Pageable pageable
    );
}
