package com.example.crackcs.evaluation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class EvaluationOperationLogger {

    public void retrievalCompleted(
            Long evaluationId,
            Long answerId,
            Long memberId,
            int candidateCount,
            List<Long> evidenceIds
    ) {
        log.info(
                "event=evaluation_retrieval_completed evaluationId={} answerId={} memberId={} "
                        + "candidateCount={} evidenceIds={}",
                evaluationId, answerId, memberId, candidateCount, evidenceIds
        );
    }

    public void evaluationCompleted(
            Long evaluationId,
            Long answerId,
            Long memberId,
            String model,
            String evaluatorVersion
    ) {
        log.info(
                "event=evaluation_completed evaluationId={} answerId={} memberId={} "
                        + "model={} evaluatorVersion={}",
                evaluationId, answerId, memberId, model, evaluatorVersion
        );
    }

    public void evaluationFailed(
            Long evaluationId,
            Long answerId,
            Long memberId,
            String failureCode
    ) {
        log.warn(
                "event=evaluation_failed evaluationId={} answerId={} memberId={} "
                        + "failureCode={}",
                evaluationId, answerId, memberId, failureCode
        );
    }
}
