package com.example.crackcs.content.concept.repository;

import com.example.crackcs.content.concept.domain.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Concept> findAllByTopicIdOrderByNameAsc(Long topicId);

    @Query("""
            SELECT concept
            FROM Concept concept
            WHERE (:topicId IS NULL OR concept.topic.id = :topicId)
              AND (:active IS NULL OR concept.active = :active)
            """)
    Page<Concept> findAllByConditions(
            @Param("topicId") Long topicId,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
