package com.example.crackcs.content.concept.repository;

import com.example.crackcs.content.concept.domain.Concept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    boolean existsByCode(String code);

    List<Concept> findAllByTopicIdOrderByNameAsc(Long topicId);
}
