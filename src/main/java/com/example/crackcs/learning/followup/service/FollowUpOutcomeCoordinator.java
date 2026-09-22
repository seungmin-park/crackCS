package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.followup.domain.FollowUpGeneration;
import com.example.crackcs.learning.followup.domain.FollowUpGenerationResult;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import com.example.crackcs.learning.followup.repository.FollowUpGenerationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class FollowUpOutcomeCoordinator {

    private final AnswerRepository answerRepository;
    private final EvaluationRepository evaluationRepository;
    private final FollowUpGenerationRepository followUpGenerationRepository;
    private final QuestionRepository questionRepository;
    private final FollowUpSourcePolicy followUpSourcePolicy;
    private final FollowUpCompletionTransaction followUpCompletionTransaction;
    private final TransactionTemplate transactionTemplate;
    @Value("${crackcs.followup.retry-delay:5s}")
    private Duration retryDelay;

    void apply(Long answerId, String token, FollowUpGenerationAttemptOutcome outcome) {
        switch (outcome) {
            case FollowUpGenerationAttemptOutcome.Completed completed ->
                    applyCompleted(answerId, token, completed.generationResult());
            case FollowUpGenerationAttemptOutcome.RetryRequired retry ->
                    applyRetry(answerId, token, retry.reason());
            case FollowUpGenerationAttemptOutcome.FailureRequired failure ->
                    applyFailure(answerId, token, failure.reason());
        }
    }

    private void applyCompleted(Long answerId, String token, FollowUpGenerationResult generationResult) {
        try {
            followUpCompletionTransaction.execute(() -> completeIfOwned(answerId, token, generationResult));
        } catch (RuntimeException storageFailure) {
            applyFailure(answerId, token, FollowUpReason.PERSISTENCE_ERROR);
        }
    }

    private void completeIfOwned(Long answerId, String token, FollowUpGenerationResult generationResult) {
        answerRepository.findLockedById(answerId).orElseThrow();
        FollowUpGeneration generation = followUpGenerationRepository.findByAnswerId(answerId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        if (!generation.hasActiveLease(token, now)) {
            return;
        }
        Evaluation evaluation = evaluationRepository.findByAnswerId(answerId).orElseThrow();
        Optional<FollowUpReason> reason = followUpSourcePolicy.findUnavailabilityReason(evaluation);
        if (reason.isPresent()) {
            generation.unavailable(token, now, reason.orElseThrow());
            return;
        }
        FollowUpRequest currentRequest = followUpSourcePolicy.request(evaluation);
        if (!isStillWithinApprovedSource(currentRequest, generationResult)) {
            generation.unavailable(token, now, FollowUpReason.CONTENT_UNAVAILABLE);
            return;
        }
        Question question = createGeneratedQuestion(generation, evaluation, generationResult);
        generation.complete(token, now, question, generationResult);
    }

    private Question createGeneratedQuestion(
            FollowUpGeneration generation,
            Evaluation evaluation,
            FollowUpGenerationResult generationResult
    ) {
        Concept concept = findGeneratedConcept(evaluation, generationResult.conceptId());
        return questionRepository.save(Question.followUpBuilder().sourceAnswer(generation.getAnswer())
                .concept(concept)
                .content(generationResult.content())
                .referenceAnswer(generationResult.referenceAnswer())
                .build());
    }

    private Concept findGeneratedConcept(Evaluation evaluation, Long conceptId) {
        return evaluation.getAnswer().getQuestion().getQuestionConcepts().stream()
                .map(questionConcept -> questionConcept.getConcept())
                .filter(concept -> concept.getId().equals(conceptId))
                .findFirst().orElseThrow();
    }

    private boolean isStillWithinApprovedSource(
            FollowUpRequest currentRequest,
            FollowUpGenerationResult generationResult
    ) {
        return currentRequest.conceptId().equals(generationResult.conceptId())
                && currentRequest.evidence().stream().map(FollowUpRequest.Evidence::chunkId).toList()
                .containsAll(generationResult.evidenceChunkIds());
    }

    private void applyRetry(Long answerId, String token, FollowUpReason reason) {
        updateIfOwned(answerId, token, (generation, now) ->
                generation.retry(token, now, reason, retryDelay));
    }

    private void applyFailure(Long answerId, String token, FollowUpReason reason) {
        updateIfOwned(answerId, token, (generation, now) -> generation.fail(token, now, reason));
    }

    private void updateIfOwned(Long answerId, String token, GenerationUpdate update) {
        transactionTemplate.executeWithoutResult(status -> {
            answerRepository.findLockedById(answerId).orElseThrow();
            FollowUpGeneration generation = followUpGenerationRepository.findByAnswerId(answerId).orElseThrow();
            LocalDateTime now = LocalDateTime.now();
            if (generation.hasActiveLease(token, now)) {
                update.apply(generation, now);
            }
        });
    }

    @FunctionalInterface
    private interface GenerationUpdate {
        void apply(FollowUpGeneration generation, LocalDateTime now);
    }
}
