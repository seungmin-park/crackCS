package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;

import java.util.List;

public interface QuestionService {

    Question create(Long topicId, QuestionDifficulty difficulty, String content, String referenceAnswer);

    List<Question> findAll();

    Question findById(Long questionId);

    Question update(Long questionId, Long topicId, QuestionDifficulty difficulty, String content, String referenceAnswer);
}
