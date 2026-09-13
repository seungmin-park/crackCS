package com.example.crackcs.learning.mastery.repository;

import com.example.crackcs.learning.mastery.domain.KnowledgeState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeStateRepository extends JpaRepository<KnowledgeState, Long> {
    List<KnowledgeState> findByMemberId(Long memberId);

    Optional<KnowledgeState> findByMemberIdAndConceptId(Long memberId, Long conceptId);
}
