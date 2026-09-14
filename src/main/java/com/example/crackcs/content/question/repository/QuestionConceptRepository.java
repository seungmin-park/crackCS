package com.example.crackcs.content.question.repository;

import com.example.crackcs.content.question.domain.QuestionConcept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionConceptRepository extends JpaRepository<QuestionConcept, Long> {

    List<QuestionConcept> findAllByQuestionId(Long questionId);
}
