package com.example.crackcs.content.concept.service;

import com.example.crackcs.content.concept.domain.Concept;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConceptService {

    Concept create(Long topicId, String code, String name, String description);

    Page<Concept> findAll(Long topicId, Boolean active, Pageable pageable);

    Concept findById(Long conceptId);

    Concept update(Long conceptId, Long topicId, String code, String name, String description);

    void deactivate(Long conceptId);
}
