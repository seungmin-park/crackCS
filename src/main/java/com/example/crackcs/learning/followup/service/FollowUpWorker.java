package com.example.crackcs.learning.followup.service;

import com.example.crackcs.content.question.domain.QuestionType;
import com.example.crackcs.evaluation.domain.EvaluationStatus;
import com.example.crackcs.learning.followup.domain.FollowUpStatus;
import com.example.crackcs.learning.followup.repository.FollowUpGenerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "crackcs.followup.worker-enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class FollowUpWorker {
    private final FollowUpGenerationRepository generations;
    private final FollowUpQuestionProcessor processor;

    @Scheduled(fixedDelayString = "${crackcs.followup.poll-delay:1000}")
    public void processPending() {
        for (Long answerId : generations.findClaimableAnswerIds(EvaluationStatus.EVALUATED, QuestionType.NORMAL,
                FollowUpStatus.PENDING, FollowUpStatus.PROCESSING, LocalDateTime.now(), PageRequest.of(0, 20))) {
            try {
                processor.process(answerId);
            } catch (RuntimeException failure) {
                log.warn("Follow-up transaction failed; answerId={}, errorType={}",
                        answerId, failure.getClass().getSimpleName());
            }
        }
    }
}
