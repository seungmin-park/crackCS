package com.example.crackcs.learning.repository;

import com.example.crackcs.learning.domain.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    Optional<Answer> findByMemberIdAndRequestId(Long memberId, String requestId);
    Optional<Answer> findByIdAndMemberId(Long id, Long memberId);
    @EntityGraph(attributePaths = "question")
    Page<Answer> findByMemberId(Long memberId, Pageable pageable);
}
