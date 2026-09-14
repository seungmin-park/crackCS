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
    @Query("SELECT question FROM Question question WHERE question.id = :questionId AND question.type = :type")
    Optional<Question> findWithConceptsByIdAndType(@Param("questionId") Long questionId, @Param("type") QuestionType type);

    default Optional<Question> findNormalWithConceptsById(Long questionId) {
        return findWithConceptsByIdAndType(questionId, QuestionType.NORMAL);
    }

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
            WHERE question.type = :type
              AND (:topicId IS NULL OR question.topic.id = :topicId)
              AND (:status IS NULL OR question.status = :status)
              AND (:difficulty IS NULL OR question.difficulty = :difficulty)
              AND (:origin IS NULL OR question.origin = :origin)
            """)
    Page<Question> findByTypeAndConditions(
            @Param("topicId") Long topicId,
            @Param("status") QuestionStatus status,
            @Param("difficulty") QuestionDifficulty difficulty,
            @Param("origin") QuestionOrigin origin,
            @Param("type") QuestionType type,
            Pageable pageable
    );

    default Page<Question> findNormalByConditions(Long topicId, QuestionStatus status, QuestionDifficulty difficulty,
                                               QuestionOrigin origin, Pageable pageable) {
        return findByTypeAndConditions(topicId, status, difficulty, origin, QuestionType.NORMAL, pageable);
    }

    @Query(
            value = """
                    SELECT question
                    FROM Question question
                    JOIN FETCH question.topic
                    WHERE question.status = :status
                      AND question.type = :type
                      AND (:topicId IS NULL OR question.topic.id = :topicId)
                      AND (:difficulty IS NULL OR question.difficulty = :difficulty)
                    """,
            countQuery = """
                    SELECT COUNT(question)
                    FROM Question question
                    WHERE question.status = :status
                      AND question.type = :type
                      AND (:topicId IS NULL OR question.topic.id = :topicId)
                      AND (:difficulty IS NULL OR question.difficulty = :difficulty)
                    """
    )
    Page<Question> findByStatusAndType(
            @Param("topicId") Long topicId,
            @Param("difficulty") QuestionDifficulty difficulty,
            @Param("status") QuestionStatus status,
            @Param("type") QuestionType type,
            Pageable pageable
    );

    default Page<Question> findPublishedNormalQuestions(Long topicId, QuestionDifficulty difficulty, Pageable pageable) {
        return findByStatusAndType(topicId, difficulty, QuestionStatus.PUBLISHED, QuestionType.NORMAL, pageable);
    }

    @Query("""
            SELECT question
            FROM Question question
            JOIN FETCH question.topic
            WHERE question.id = :questionId
              AND question.status = :status
              AND question.type = :type
            """)
    Optional<Question> findByIdAndStatusAndType(@Param("questionId") Long questionId, @Param("status") QuestionStatus status,
                                        @Param("type") QuestionType type);

    default Optional<Question> findPublishedNormalById(Long questionId) {
        return findByIdAndStatusAndType(questionId, QuestionStatus.PUBLISHED, QuestionType.NORMAL);
    }
}
