package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.learning.answer.domain.Answer;
import com.example.crackcs.learning.answer.repository.AnswerRepository;
import com.example.crackcs.learning.followup.domain.FollowUpGeneration;
import com.example.crackcs.learning.followup.domain.FollowUpReason;
import com.example.crackcs.learning.followup.port.FollowUpRequest;
import com.example.crackcs.learning.followup.repository.FollowUpGenerationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final AnswerRepository answerRepository;
    private final EvaluationRepository evaluationRepository;
    private final FollowUpGenerationRepository followUpGenerationRepository;
    private final FollowUpSourcePolicy followUpSourcePolicy;
    private final TransactionTemplate transactionTemplate;
    private final FollowUpGenerationAttemptExecutor followUpGenerationAttemptExecutor;
    private final FollowUpOutcomeCoordinator followUpOutcomeCoordinator;
    @Value("${crackcs.followup.lease-duration:1m}")
    private Duration leaseDuration;

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
        FollowUpGenerationAttemptOutcome outcome =
                followUpGenerationAttemptExecutor.execute(claimed.orElseThrow());
        followUpOutcomeCoordinator.apply(answerId, token, outcome);
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
        FollowUpGeneration generation = followUpGenerationRepository.findByAnswerId(answer.getId())
                .orElseGet(() -> followUpGenerationRepository.save(FollowUpGeneration.builder().answer(answer).build()));
        LocalDateTime now = LocalDateTime.now();
        if (!generation.claim(token, now, leaseDuration)) {
            return Optional.empty();
        }
        Optional<FollowUpReason> reason = followUpSourcePolicy.findUnavailabilityReason(evaluation);
        if (reason.isPresent()) {
            generation.unavailable(token, now, reason.orElseThrow());
            return Optional.empty();
        }
        return Optional.of(followUpSourcePolicy.request(evaluation));
    }

    private boolean isNormalQuestionAnswer(Answer answer) {
        return answer.getQuestion().getType() == QuestionType.NORMAL;
    }

    private boolean hasCompletedEvaluation(Evaluation evaluation) {
        return evaluation.getStatus() == EvaluationStatus.EVALUATED;
    }

}
