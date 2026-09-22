package com.example.crackcs.evaluation.service;

import com.example.crackcs.content.question.domain.Question;
import com.example.crackcs.evaluation.domain.Evaluation;
import com.example.crackcs.evaluation.port.EvaluationConceptInput;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import com.example.crackcs.learning.answer.domain.Answer;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DefaultEvaluationProcessor implements EvaluationProcessor {

    private final EvaluationRepository evaluationRepository;
    private final TransactionTemplate transactionTemplate;
    private final EvaluationAttemptExecutor evaluationAttemptExecutor;
    private final EvaluationOutcomeCoordinator evaluationOutcomeCoordinator;
    private final String workerId = UUID.randomUUID().toString();
    private final Object[] localLocks = IntStream.range(0, 64).mapToObj(ignored -> new Object()).toArray();
    @Value("${crackcs.evaluation.lease-duration:1m}")
    private Duration leaseDuration;

    @Override
    @Timed(value = "crackcs.evaluation.process", description = "Evaluation processing time")
    public void process(Long evaluationId) {
        Object localLock = localLocks[Math.floorMod(evaluationId.hashCode(), localLocks.length)];
        synchronized (localLock) {
            processWithLocalLock(evaluationId);
        }
    }

    private void processWithLocalLock(Long evaluationId) {
        Optional<ClaimedEvaluationWork> claimed = Objects.requireNonNull(
                transactionTemplate.execute(status -> claim(evaluationId)),
                "claim transaction must return an Optional");
        if (claimed.isEmpty()) {
            return;
        }
        ClaimedEvaluationWork claimedWork = claimed.orElseThrow();
        EvaluationAttemptOutcome outcome = evaluationAttemptExecutor.execute(claimedWork);
        evaluationOutcomeCoordinator.apply(workerId, claimedWork, outcome);
    }

    private Optional<ClaimedEvaluationWork> claim(Long evaluationId) {
        Optional<Evaluation> found = evaluationRepository.findLockedById(evaluationId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Evaluation evaluation = found.orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        if (!evaluation.claim(workerId, now, leaseDuration)) {
            return Optional.empty();
        }
        return Optional.of(toClaimedWork(evaluation));
    }

    private ClaimedEvaluationWork toClaimedWork(Evaluation evaluation) {
        Answer answer = evaluation.getAnswer();
        Question question = answer.getQuestion();
        List<EvaluationConceptInput> concepts = question.getQuestionConcepts().stream()
                .map(qc -> new EvaluationConceptInput(qc.getConcept().getId(), qc.getConcept().getName(),
                        qc.isRequired()))
                .toList();
        return new ClaimedEvaluationWork(
                evaluation.getId(),
                answer.getId(), answer.getMember().getId(),
                question.getTopic().getId(), question.getContent(), question.getReferenceAnswer(),
                answer.getContent(), concepts, evaluation.getAttemptCount()
        );
    }

}
