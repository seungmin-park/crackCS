package com.example.crackcs.content.question.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionDifficulty;
import com.example.crackcs.content.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<Question> findAll() {
        return questionRepository.findAll();
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
