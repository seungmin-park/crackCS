package com.example.crackcs.content.question.repository;

import com.example.crackcs.content.question.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
