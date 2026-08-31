package com.example.crackcs.member.service;

import com.example.crackcs.member.domain.Member;

public interface MemberService {

    Member findById(Long memberId);
}
