package com.example.crackcs.member.service;

import com.example.crackcs.exception.MemberNotFoundException;
import com.example.crackcs.member.domain.Member;
import com.example.crackcs.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultMemberService implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    @Override
    public Page<Member> findAll(MemberRole role, MemberStatus status, Pageable pageable) {
        return memberRepository.findAllByConditions(role, status, pageable);
    }

    @Override
    @Transactional
    public Member changeStatus(Long memberId, MemberStatus status) {
        Member member = findById(memberId);
        member.changeStatus(status);
        return member;
    }
}
