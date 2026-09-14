package com.example.crackcs.learning.recommendation.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionOrigin;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.answer.repository.LastAnsweredQuestion;
import com.example.crackcs.learning.mastery.domain.KnowledgeState;
import com.example.crackcs.learning.mastery.repository.KnowledgeStateRepository;
import com.example.crackcs.learning.recommendation.domain.RecommendationCandidate;
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult;
import com.example.crackcs.learning.recommendation.service.result.RecommendationResult.Reason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultRecommendationService implements RecommendationService {
    private final KnowledgeStateRepository knowledgeStateRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Override
    public RecommendationResult recommendation(Long memberId) {
        Map<Long, KnowledgeState> memberStates = knowledgeStateRepository.findByMemberId(memberId).stream()
                .collect(Collectors.toMap(state -> state.getConcept().getId(), Function.identity()));
        Map<Long, LocalDateTime> lastAnswered = answerRepository.findLastAnsweredByQuestion(memberId).stream()
                .collect(
                        Collectors.toMap(LastAnsweredQuestion::getQuestionId, LastAnsweredQuestion::getLastAnsweredAt));
        return questionRepository.findAvailableForRecommendation(QuestionStatus.PUBLISHED, QuestionType.NORMAL,
                        QuestionOrigin.ADMIN)
                .stream().flatMap(question -> candidates(question, memberStates, lastAnswered))
                .min(Comparator.naturalOrder()).map(this::response).orElseGet(this::unavailable);
    }

    private Stream<RecommendationCandidate> candidates(Question question, Map<Long, KnowledgeState> statesByConceptId,
                                                       Map<Long, LocalDateTime> lastAnswered) {
        return question.getQuestionConcepts().stream().map(link -> RecommendationCandidate.from(
                question, link.getConcept(), statesByConceptId.get(link.getConcept().getId()),
                lastAnswered.get(question.getId())));
    }

    private RecommendationResult response(RecommendationCandidate candidate) {
        return new RecommendationResult(candidate.question().getId(), candidate.question().getContent(),
                candidate.concept().getId(), candidate.concept().getName(),
                candidate.isUnassessed() ? Reason.UNASSESSED_CONCEPT : Reason.LOW_MASTERY,
                candidate.isUnassessed() ? "아직 평가하지 않은 개념을 확인해 보세요." : "숙련도가 낮은 개념부터 다시 연습해 보세요.");
    }

    private RecommendationResult unavailable() {
        return new RecommendationResult(null, null, null, null, Reason.NO_AVAILABLE_QUESTION,
                "현재 학습할 수 있는 공개 문제가 없습니다.");
    }
}
