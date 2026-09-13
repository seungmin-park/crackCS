package com.example.crackcs.evaluation.port;

import com.example.crackcs.evaluation.domain.EvaluationResult;

public interface EvaluationPort {
    EvaluationResult evaluate(EvaluationRequest request);
}
