package com.example.crackcs.evaluation.port;

/**
 * 완료된 개념 판정을 후속 학습 상태에 반영하는 계약. 호출자의 평가 완료 transaction에 참여하며, 실패를 전파해 전체 반영을 rollback한다.
 */
public interface EvaluatedConceptApplicationPort {
    void applyInCurrentTransaction(Long evaluationId);
}
