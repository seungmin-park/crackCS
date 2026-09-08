package com.example.crackcs.evaluation.service;

import com.example.crackcs.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(name = "crackcs.evaluation.worker-enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@RequiredArgsConstructor
public class EvaluationWorker {

    private final EvaluationRepository evaluations;
    private final EvaluationProcessor processor;

    @Scheduled(fixedDelayString = "${crackcs.evaluation.poll-delay:1000}")
    public void processPending() {
        for (Long id : evaluations.findClaimableIds(LocalDateTime.now(), PageRequest.of(0, 20))) {
            try {
                processor.process(id);
            } catch (RuntimeException failure) {
                // A rolled-back transaction leaves durable EVALUATING work for the next scan.
                log.warn(
                        "Evaluation transaction failed; evaluationId={}, errorType={}",
                        id,
                        failure.getClass().getSimpleName()
                );
            }
        }
    }
}
