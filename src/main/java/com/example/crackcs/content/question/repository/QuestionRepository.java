package com.example.crackcs.content.question.repository;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
            SELECT question
            FROM Question question
            WHERE (:topicId IS NULL OR question.topicId = :topicId)
              AND (:status IS NULL OR question.status = :status)
              AND (:difficulty IS NULL OR question.difficulty = :difficulty)
              AND (:origin IS NULL OR question.origin = :origin)
            """)
    Page<Question> findAllByConditions(
            @Param("topicId") Long topicId,
            @Param("status") QuestionStatus status,
            @Param("difficulty") QuestionDifficulty difficulty,
            @Param("origin") QuestionOrigin origin,
            Pageable pageable
    );
}
