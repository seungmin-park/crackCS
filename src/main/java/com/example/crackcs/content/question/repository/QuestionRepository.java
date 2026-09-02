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
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = {"questionConcepts", "questionConcepts.concept"})
    @Query("SELECT question FROM Question question WHERE question.id = :questionId")
    Optional<Question> findAdminById(@Param("questionId") Long questionId);

    @Query("""
            SELECT COALESCE(MAX(question.questionVersion), 0)
            FROM Question question
            WHERE question.versionSeriesId = :versionSeriesId
            """)
    int findMaxVersion(@Param("versionSeriesId") String versionSeriesId);

    List<Question> findAllByVersionSeriesIdAndStatus(String versionSeriesId, QuestionStatus status);

    @Query("""
            SELECT question
            FROM Question question
            WHERE (:topicId IS NULL OR question.topic.id = :topicId)
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

    @Query(
            value = """
                    SELECT question
                    FROM Question question
                    JOIN FETCH question.topic
                    WHERE question.status = com.example.crackcs.content.question.domain.QuestionStatus.PUBLISHED
                      AND (:topicId IS NULL OR question.topic.id = :topicId)
                      AND (:difficulty IS NULL OR question.difficulty = :difficulty)
                    """,
            countQuery = """
                    SELECT COUNT(question)
                    FROM Question question
                    WHERE question.status = com.example.crackcs.content.question.domain.QuestionStatus.PUBLISHED
                      AND (:topicId IS NULL OR question.topic.id = :topicId)
                      AND (:difficulty IS NULL OR question.difficulty = :difficulty)
                    """
    )
    Page<Question> findPublishedQuestions(
            @Param("topicId") Long topicId,
            @Param("difficulty") QuestionDifficulty difficulty,
            Pageable pageable
    );

    @Query("""
            SELECT question
            FROM Question question
            JOIN FETCH question.topic
            WHERE question.id = :questionId
              AND question.status = com.example.crackcs.content.question.domain.QuestionStatus.PUBLISHED
            """)
    Optional<Question> findPublishedById(@Param("questionId") Long questionId);
}
