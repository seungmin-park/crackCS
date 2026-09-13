package com.example.crackcs.content.question.repository;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.domain.QuestionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("""
            select distinct q from Question q join fetch q.topic t
            join fetch q.questionConcepts qc join fetch qc.concept c
            where q.status = :status and q.type = :type and q.origin = :origin
              and t.active = true
              and not exists (select invalid.id from QuestionConcept invalid
                  where invalid.question = q and (invalid.concept.active = false or invalid.concept.topic.active = false))
            """)
    List<Question> findAvailableForRecommendation(@Param("status") QuestionStatus status,
                                                  @Param("type") QuestionType type,
                                                  @Param("origin") QuestionOrigin origin);

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
                    WHERE question.status = :status
                      AND (:topicId IS NULL OR question.topic.id = :topicId)
                      AND (:difficulty IS NULL OR question.difficulty = :difficulty)
                    """,
            countQuery = """
                    SELECT COUNT(question)
                    FROM Question question
                    WHERE question.status = :status
                      AND (:topicId IS NULL OR question.topic.id = :topicId)
                      AND (:difficulty IS NULL OR question.difficulty = :difficulty)
                    """
    )
    Page<Question> findQuestionsByStatus(
            @Param("topicId") Long topicId,
            @Param("difficulty") QuestionDifficulty difficulty,
            @Param("status") QuestionStatus status,
            Pageable pageable
    );

    default Page<Question> findPublishedQuestions(Long topicId, QuestionDifficulty difficulty, Pageable pageable) {
        return findQuestionsByStatus(topicId, difficulty, QuestionStatus.PUBLISHED, pageable);
    }

    @Query("""
            SELECT question
            FROM Question question
            JOIN FETCH question.topic
            WHERE question.id = :questionId
              AND question.status = :status
            """)
    Optional<Question> findByIdAndStatus(@Param("questionId") Long questionId, @Param("status") QuestionStatus status);

    default Optional<Question> findPublishedById(Long questionId) {
        return findByIdAndStatus(questionId, QuestionStatus.PUBLISHED);
    }
}
