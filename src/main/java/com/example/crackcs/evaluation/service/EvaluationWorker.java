package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("(local | test) & !prod & !production")
@ConditionalOnProperty(name = "crackcs.evaluation.worker-enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@RequiredArgsConstructor
public class EvaluationWorker {
    private static final Logger log = LoggerFactory.getLogger(EvaluationWorker.class);
    private final EvaluationRepository evaluations;
    private final EvaluationProcessor processor;

    @Scheduled(fixedDelayString = "${crackcs.evaluation.poll-delay:1000}")
    public void processPending() {
        for (Long id : evaluations.findPendingIds(EvaluationStatus.EVALUATING, PageRequest.of(0, 20))) {
            try {
                processor.process(id);
            } catch (RuntimeException failure) {
                // A rolled-back transaction leaves durable EVALUATING work for the next scan.
                log.warn("Evaluation transaction failed; evaluationId={}, errorType={}", id, failure.getClass().getSimpleName());
            }
        }
    }
}
