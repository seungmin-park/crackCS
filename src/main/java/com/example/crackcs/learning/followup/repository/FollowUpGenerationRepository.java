package com.example.crackcs.learning.followup.repository;

import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.learning.followup.domain.FollowUpGeneration;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FollowUpGenerationRepository extends JpaRepository<FollowUpGeneration, Long> {
    Optional<FollowUpGeneration> findByAnswerId(Long answerId);

    @Query("""
            select e.answer.id from Evaluation e
            left join FollowUpGeneration g on g.answer = e.answer
            where e.status = :evaluated and e.answer.question.type = :normal
              and (g.id is null or (g.status = :pending and g.nextAttemptAt <= :now)
                   or (g.status = :processing and g.leaseExpiresAt <= :now))
            order by e.id
            """)
    List<Long> findClaimableAnswerIds(@Param("evaluated") EvaluationStatus evaluated,
                                      @Param("normal") QuestionType normal, @Param("pending") FollowUpStatus pending,
                                      @Param("processing") FollowUpStatus processing, @Param("now") LocalDateTime now, Pageable pageable);
}
