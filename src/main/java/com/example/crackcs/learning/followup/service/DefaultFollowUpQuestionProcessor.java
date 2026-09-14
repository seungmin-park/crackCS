package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.concept.domain.Concept;
import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.content.question.repository.QuestionRepository;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.exception.EvaluationTimeoutException;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.followup.domain.FollowUpGeneration;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.domain.FollowUpResult;
import com.example.crackcs.learning.followup.port.FollowUpQuestionGenerator;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import com.example.crackcs.learning.followup.repository.FollowUpGenerationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultFollowUpQuestionProcessor implements FollowUpQuestionProcessor {
    private static final int MAX_COMPLETION_ATTEMPTS = 3;
    private final AnswerRepository answerRepository;
    private final EvaluationRepository evaluationRepository;
    private final FollowUpGenerationRepository followUpGenerationRepository;
    private final QuestionRepository questionRepository;
    private final FollowUpSourcePolicy policy;
    private final ObjectProvider<FollowUpQuestionGenerator> generators;
    private final TransactionTemplate transactionTemplate;
    @Value("${crackcs.followup.lease-duration:1m}")
    private Duration leaseDuration;
    @Value("${crackcs.followup.retry-delay:5s}")
    private Duration retryDelay;

    @Transactional(propagation = Propagation.NEVER)
    public void process(Long answerId) {
        String token = UUID.randomUUID().toString();
        // 선점 transaction을 끝낸 뒤 AI를 호출해, 외부 응답 대기 중에는 DB 잠금을 잡지 않는다.
        Optional<FollowUpRequest> claimed = Objects.requireNonNull(
                transactionTemplate.execute(status -> claim(answerId, token)),
                "claim transaction must return an Optional");
        if (claimed.isEmpty()) {
            return;
        }
        FollowUpRequest request = claimed.orElseThrow();
        FollowUpResult result;
        try {
            FollowUpQuestionGenerator generator = generators.getIfAvailable();
            if (generator == null) {
                failed(answerId, token, FollowUpReason.PROVIDER_ERROR, true);
                return;
            }
            result = generator.generate(request);
            if (result == null) {
                throw new IllegalArgumentException("follow-up result is required");
            }
            result.validateAgainst(request.conceptId(), request.evidence().stream().map(FollowUpRequest.Evidence::chunkId).toList());
        } catch (EvaluationTimeoutException timeout) {
            failed(answerId, token, FollowUpReason.PROVIDER_TIMEOUT, true);
            return;
        } catch (IllegalArgumentException invalid) {
            failed(answerId, token, FollowUpReason.INVALID_RESULT, false);
            return;
        } catch (RuntimeException providerFailure) {
            failed(answerId, token, FollowUpReason.PROVIDER_ERROR, true);
            return;
        }
        try {
            completeWithRetry(answerId, token, result);
        } catch (RuntimeException storageFailure) {
            failed(answerId, token, FollowUpReason.PERSISTENCE_ERROR, false);
        }
    }

    private void completeWithRetry(Long answerId, String token, FollowUpResult result) {
        for (int attempt = 1; ; attempt++) {
            try {
                // process의 NEVER 계약으로 각 시도는 별도 transaction이다. AI 결과는 재사용한다.
                transactionTemplate.executeWithoutResult(status -> complete(answerId, token, result));
                return;
            } catch (OptimisticLockingFailureException | PessimisticLockingFailureException conflict) {
                if (attempt == MAX_COMPLETION_ATTEMPTS) {
                    throw conflict;
                }
            }
        }
    }

    private Optional<FollowUpRequest> claim(Long answerId, String token) {
        Optional<Answer> answer = answerRepository.findLockedById(answerId).filter(this::isNormalQuestionAnswer);
        if (answer.isEmpty()) {
            return Optional.empty();
        }
        Optional<Evaluation> evaluation = evaluationRepository.findByAnswerId(answerId).filter(this::hasCompletedEvaluation);
        if (evaluation.isEmpty()) {
            return Optional.empty();
        }
        return claimGeneration(answer.orElseThrow(), evaluation.orElseThrow(), token);
    }

    private Optional<FollowUpRequest> claimGeneration(Answer answer, Evaluation evaluation, String token) {
        FollowUpGeneration job = followUpGenerationRepository.findByAnswerId(answer.getId())
                .orElseGet(() -> followUpGenerationRepository.save(FollowUpGeneration.builder().answer(answer).build()));
        LocalDateTime now = LocalDateTime.now();
        if (!job.claim(token, now, leaseDuration)) {
            return Optional.empty();
        }
        Optional<FollowUpReason> reason = policy.findUnavailabilityReason(evaluation);
        if (reason.isPresent()) {
            job.unavailable(token, now, reason.orElseThrow());
            return Optional.empty();
        }
        return Optional.of(policy.request(evaluation));
    }

    private void complete(Long answerId, String token, FollowUpResult result) {
        answerRepository.findLockedById(answerId).orElseThrow();
        FollowUpGeneration job = followUpGenerationRepository.findByAnswerId(answerId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        if (!job.hasActiveLease(token, now)) {
            return;
        }
        Evaluation evaluation = evaluationRepository.findByAnswerId(answerId).orElseThrow();
        Optional<FollowUpReason> reason = policy.findUnavailabilityReason(evaluation);
        if (reason.isPresent()) {
            job.unavailable(token, now, reason.orElseThrow());
            return;
        }
        FollowUpRequest current = policy.request(evaluation);
        if (!isStillWithinApprovedSource(current, result)) {
            job.unavailable(token, now, FollowUpReason.CONTENT_UNAVAILABLE);
            return;
        }
        Question question = createGeneratedQuestion(job, evaluation, result);
        job.complete(token, now, question, result);
    }

    private Question createGeneratedQuestion(FollowUpGeneration job, Evaluation evaluation, FollowUpResult result) {
        Concept concept = findGeneratedConcept(evaluation, result.conceptId());
        return questionRepository.save(Question.followUpBuilder().sourceAnswer(job.getAnswer())
                .concept(concept).content(result.content()).referenceAnswer(result.referenceAnswer()).build());
    }

    private Concept findGeneratedConcept(Evaluation evaluation, Long conceptId) {
        return evaluation.getAnswer().getQuestion().getQuestionConcepts().stream()
                .map(questionConcept -> questionConcept.getConcept())
                .filter(concept -> concept.getId().equals(conceptId))
                .findFirst().orElseThrow();
    }

    private boolean isNormalQuestionAnswer(Answer answer) {
        return answer.getQuestion().getType() == QuestionType.NORMAL;
    }

    private boolean hasCompletedEvaluation(Evaluation evaluation) {
        return evaluation.getStatus() == EvaluationStatus.EVALUATED;
    }

    private boolean isStillWithinApprovedSource(FollowUpRequest current, FollowUpResult result) {
        return current.conceptId().equals(result.conceptId())
                && current.evidence().stream().map(FollowUpRequest.Evidence::chunkId).toList()
                .containsAll(result.evidenceChunkIds());
    }

    private void failed(Long answerId, String token, FollowUpReason reason, boolean retry) {
        transactionTemplate.executeWithoutResult(status -> {
            answerRepository.findLockedById(answerId).orElseThrow();
            FollowUpGeneration job = followUpGenerationRepository.findByAnswerId(answerId).orElseThrow();
            LocalDateTime now = LocalDateTime.now();
            if (job.hasActiveLease(token, now)) {
                if (retry) {
                    job.retry(token, now, reason, retryDelay);
                } else {
                    job.fail(token, now, reason);
                }
            }
        });
    }
}
