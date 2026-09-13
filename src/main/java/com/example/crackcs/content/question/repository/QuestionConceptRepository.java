package com.example.crackcs.content.question.repository;

import com.example.crackcs.content.question.domain.QuestionConcept;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionConceptRepository extends JpaRepository<QuestionConcept, Long> {

    List<QuestionConcept> findAllByQuestionId(Long questionId);
}
