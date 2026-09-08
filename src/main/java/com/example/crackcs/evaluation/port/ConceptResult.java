package com.example.crackcs.evaluation.port;

import com.example.crackcs.evaluation.domain.Verdict;

public record ConceptResult(Long conceptId, Verdict verdict, String feedback) {
}
