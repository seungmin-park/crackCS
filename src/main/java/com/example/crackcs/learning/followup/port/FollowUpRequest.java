package com.example.crackcs.learning.followup.port;

import com.example.crackcs.evaluation.domain.Verdict;

import java.util.List;

public record FollowUpRequest(String question, Long conceptId, String conceptName, Verdict purpose,
                              String feedback, List<String> omissions, List<String> misconceptions,
                              List<Evidence> evidence) {
    public FollowUpRequest {
        omissions = List.copyOf(omissions);
        misconceptions = List.copyOf(misconceptions);
        evidence = List.copyOf(evidence);
    }

    public record Evidence(Long chunkId, String content) {
    }
}
