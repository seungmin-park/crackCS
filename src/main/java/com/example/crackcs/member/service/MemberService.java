package com.example.crackcs.member.service;

import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberService {

    Member findById(Long memberId);

    Page<Member> findAll(MemberRole role, MemberStatus status, Pageable pageable);

    Member changeStatus(Long memberId, MemberStatus status);
}
