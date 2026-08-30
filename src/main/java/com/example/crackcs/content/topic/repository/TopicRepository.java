package com.example.crackcs.content.topic.repository;

import com.example.crackcs.content.topic.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsByCode(String code);

    List<Topic> findAllByParentIsNullOrderByNameAsc();

    List<Topic> findAllByParentIdOrderByNameAsc(Long parentId);
}
