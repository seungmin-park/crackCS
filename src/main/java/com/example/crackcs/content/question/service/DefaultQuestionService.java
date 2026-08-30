package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultQuestionService implements QuestionService {

    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public Question create(Long topicId, QuestionDifficulty difficulty, String content, String referenceAnswer) {
        Question question = Question.builder()
                .topicId(topicId)
                .difficulty(difficulty)
                .content(content)
                .referenceAnswer(referenceAnswer)
                .build();

        return questionRepository.save(question);
    }

    @Override
    public Page<Question> findAll(
            Long topicId,
            QuestionStatus status,
            QuestionDifficulty difficulty,
            QuestionOrigin origin,
            Pageable pageable
    ) {
        return questionRepository.findAllByConditions(topicId, status, difficulty, origin, pageable);
    }

    @Override
    public Question findById(Long questionId) {
        return findQuestion(questionId);
    }

    @Override
    @Transactional
    public Question update(
            Long questionId,
            Long topicId,
            QuestionDifficulty difficulty,
            String content,
            String referenceAnswer
    ) {
        Question question = findQuestion(questionId);
        question.update(topicId, difficulty, content, referenceAnswer);

        return questionRepository.save(question);
    }

    private Question findQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }
}
