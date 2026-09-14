package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionStatus;
import com.example.crackcs.content.topic.domain.Topic;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.exception.AnswerNotFoundException;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.followup.domain.FollowUpGeneration;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;
import com.example.crackcs.learning.followup.repository.FollowUpGenerationRepository;
import com.example.crackcs.learning.followup.service.FollowUpQuestionResult.QuestionResult;
import com.example.crackcs.learning.followup.service.FollowUpQuestionResult.TopicResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultFollowUpQuestionService implements FollowUpQuestionService {
    private final AnswerRepository answerRepository;
    private final EvaluationRepository evaluationRepository;
    private final FollowUpGenerationRepository followUpGenerationRepository;
    private final FollowUpSourcePolicy policy;

    public FollowUpQuestionResult findByAnswerId(Long memberId, Long answerId) {
        answerRepository.findByIdAndMemberId(answerId, memberId).orElseThrow(() -> new AnswerNotFoundException(answerId));
        return evaluationRepository.findByAnswerId(answerId)
                .map(evaluation -> findForEvaluation(answerId, evaluation))
                .orElseGet(() -> FollowUpQuestionResult.unavailable(FollowUpReason.EVALUATION_NOT_ELIGIBLE));
    }

    private FollowUpQuestionResult findForEvaluation(Long answerId, Evaluation evaluation) {
        return policy.findUnavailabilityReason(evaluation)
                .map(FollowUpQuestionResult::unavailable)
                .orElseGet(() -> findGenerationResult(answerId, evaluation));
    }

    private FollowUpQuestionResult findGenerationResult(Long answerId, Evaluation evaluation) {
        return followUpGenerationRepository.findByAnswerId(answerId)
                .map(job -> toGenerationResult(job, evaluation))
                .orElseGet(FollowUpQuestionResult::pending);
    }

    private FollowUpQuestionResult toGenerationResult(FollowUpGeneration job, Evaluation evaluation) {
        if (job.getStatus() != FollowUpStatus.READY) {
            return FollowUpQuestionResult.withoutQuestion(job.getStatus(), job.getReason());
        }
        return findReadyQuestion(job, evaluation);
    }

    private FollowUpQuestionResult findReadyQuestion(FollowUpGeneration job, Evaluation evaluation) {
        if (!hasAvailableGeneratedQuestion(job, evaluation)) {
            return FollowUpQuestionResult.unavailable(FollowUpReason.CONTENT_UNAVAILABLE);
        }
        return readyQuestion(job.getQuestion());
    }

    private boolean hasAvailableGeneratedQuestion(FollowUpGeneration job, Evaluation evaluation) {
        return job.getQuestion().getStatus() == QuestionStatus.PUBLISHED
                && new HashSet<>(policy.availableEvidence(evaluation).stream().map(chunk -> chunk.getId()).toList())
                .containsAll(job.getEvidenceChunkIds());
    }

    private FollowUpQuestionResult readyQuestion(Question question) {
        Topic topic = question.getTopic();
        return FollowUpQuestionResult.ready(
                new QuestionResult(question.getId(), new TopicResult(topic.getId(), topic.getCode(), topic.getName()),
                        question.getDifficulty(), question.getContent()));
    }
}
