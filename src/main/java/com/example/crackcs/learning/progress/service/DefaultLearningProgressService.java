package com.example.crackcs.learning.progress.service;

import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.mastery.service.KnowledgeQueryService;
import com.example.crackcs.learning.progress.service.result.LearningProgressResult;
import com.example.crackcs.learning.progress.service.result.LearningProgressResult.RecentEvaluation;
import com.example.crackcs.learning.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultLearningProgressService implements LearningProgressService {
    private final AnswerRepository answerRepository;
    private final KnowledgeQueryService knowledgeQueryService;
    private final RecommendationService recommendationService;

    @Override
    public LearningProgressResult progress(Long memberId) {
        List<RecentEvaluation> recent = answerRepository.findRecentEvaluations(memberId, PageRequest.of(0, 5)).stream()
                .map(row -> new RecentEvaluation(row.getAnswerId(), row.getQuestionTitle(), row.getStatus(),
                        row.getVerdict(), row.getScore(), row.getSubmittedAt())).toList();
        return new LearningProgressResult(answerRepository.countSubmittedSince(memberId, null),
                answerRepository.countSubmittedSince(memberId, LocalDateTime.now().minusDays(7)), recent,
                knowledgeQueryService.knowledgeStates(memberId).topics(), recommendationService.recommendation(memberId));
    }
}
