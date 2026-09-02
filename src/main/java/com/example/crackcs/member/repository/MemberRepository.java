package com.example.crackcs.member.repository;

import com.example.crackcs.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

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
