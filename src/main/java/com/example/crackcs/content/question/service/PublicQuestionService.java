package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicQuestionService {

    Page<Question> findAll(Long topicId, QuestionDifficulty difficulty, Pageable pageable);

    Question findById(Long questionId);
}
