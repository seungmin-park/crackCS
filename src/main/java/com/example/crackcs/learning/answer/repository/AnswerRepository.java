package com.example.crackcs.learning.answer.repository;

import com.example.crackcs.learning.answer.domain.Answer;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Answer a where a.id = :id")
    Optional<Answer> findLockedById(@Param("id") Long id);

    @Query("""
            select a.question.id as questionId, max(a.submittedAt) as lastAnsweredAt from Answer a
            where a.member.id = :memberId group by a.question.id
            """)
    List<LastAnsweredQuestion> findLastAnsweredByQuestion(@Param("memberId") Long memberId);

    @Query("""
            select count(a) from Answer a where a.member.id = :memberId
            and (:since is null or a.submittedAt >= :since)
            """)
    long countSubmittedSince(@Param("memberId") Long memberId, @Param("since") LocalDateTime since);

    @Query("""
            select a.id as answerId, a.question.content as questionTitle, e.status as status,
                   e.verdict as verdict, e.score as score, a.submittedAt as submittedAt
            from Answer a left join Evaluation e on e.answer = a
            where a.member.id = :memberId order by a.submittedAt desc, a.id desc
            """)
    List<RecentAnswerEvaluation> findRecentEvaluations(@Param("memberId") Long memberId, Pageable pageable);

    Optional<Answer> findByMemberIdAndRequestId(Long memberId, String requestId);

    Optional<Answer> findByIdAndMemberId(Long id, Long memberId);

    @EntityGraph(attributePaths = "question")
    Page<Answer> findByMemberId(Long memberId, Pageable pageable);
}
