package com.example.crackcs.content.topic.repository;

import com.example.crackcs.content.topic.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByParentId(Long parentId);

    List<Topic> findAllByParentIsNullOrderByNameAsc();

    List<Topic> findAllByParentIdOrderByNameAsc(Long parentId);

    @Query("""
            SELECT topic
            FROM Topic topic
            WHERE (:parentId IS NULL OR topic.parent.id = :parentId)
              AND (:active IS NULL OR topic.active = :active)
            """)
    Page<Topic> findAllByConditions(
            @Param("parentId") Long parentId,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
