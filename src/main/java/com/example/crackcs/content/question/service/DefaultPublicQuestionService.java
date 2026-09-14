package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.exception.QuestionNotFoundException;
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
        return questionRepository.findPublishedNormalQuestions(topicId, difficulty, pageable);
    }

    @Override
    public Question findById(Long questionId) {
        return questionRepository.findPublishedNormalById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }
}
