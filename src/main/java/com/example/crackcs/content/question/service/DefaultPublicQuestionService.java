package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.content.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPublicQuestionService implements PublicQuestionService {

    private final QuestionRepository questionRepository;

    @Override
    public Page<Question> findAll(Long topicId, QuestionDifficulty difficulty, Pageable pageable) {
        return questionRepository.findPublishedQuestions(topicId, difficulty, pageable);
    }

    @Override
    public Question findById(Long questionId) {
        return questionRepository.findPublishedById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }
}
