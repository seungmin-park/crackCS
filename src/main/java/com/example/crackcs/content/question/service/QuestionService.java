package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionService {

    Question create(Long topicId, QuestionDifficulty difficulty, String content, String referenceAnswer);

    Page<Question> findAll(
            Long topicId,
            QuestionStatus status,
            QuestionDifficulty difficulty,
            QuestionOrigin origin,
            Pageable pageable
    );

    Question findById(Long questionId);

    Question update(Long questionId, Long topicId, QuestionDifficulty difficulty, String content, String referenceAnswer);
}
