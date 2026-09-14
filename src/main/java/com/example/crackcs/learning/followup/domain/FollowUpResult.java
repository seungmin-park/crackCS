package com.example.crackcs.learning.followup.domain;

import java.util.HashSet;
import java.util.List;

public record FollowUpResult(String content, String referenceAnswer, Long conceptId, List<Long> evidenceChunkIds,
                             String modelName, String generatorVersion, long durationMillis,
                             long inputTokens, long outputTokens) {
    private static final int MAX_CONTENT_LENGTH = 10_000;
    private static final int MAX_MODEL_NAME_LENGTH = 100;
    private static final String SUPPORTED_GENERATOR_VERSION = "follow-up-v1";

    public FollowUpResult {
        requireValidQuestion(content, referenceAnswer, conceptId);
        requireValidEvidenceIds(evidenceChunkIds);
        requireValidGenerationMetadata(modelName, generatorVersion, durationMillis, inputTokens, outputTokens);
        content = content.trim();
        referenceAnswer = referenceAnswer.trim();
        evidenceChunkIds = List.copyOf(evidenceChunkIds);
    }

    public void validateAgainst(Long selectedConceptId, List<Long> allowedEvidenceIds) {
        if (!usesSelectedConcept(selectedConceptId) || !usesOnlyAllowedEvidence(allowedEvidenceIds)) {
            throw new IllegalArgumentException("follow-up result exceeds approved evidence or concept");
        }
    }

    private boolean usesSelectedConcept(Long selectedConceptId) {
        return conceptId.equals(selectedConceptId);
    }

    private boolean usesOnlyAllowedEvidence(List<Long> allowedEvidenceIds) {
        return allowedEvidenceIds.containsAll(evidenceChunkIds);
    }

    private static void requireValidQuestion(String content, String referenceAnswer, Long conceptId) {
        if (!isValidText(content, MAX_CONTENT_LENGTH) || !isValidText(referenceAnswer, MAX_CONTENT_LENGTH)
                || !isPositiveId(conceptId)) {
            throw invalidResult();
        }
    }

    private static void requireValidEvidenceIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw invalidResult();
        }
        if (!containsOnlyPositiveIds(ids) || containsDuplicateIds(ids)) {
            throw invalidResult();
        }
    }

    private static boolean containsOnlyPositiveIds(List<Long> ids) {
        return ids.stream().allMatch(FollowUpResult::isPositiveId);
    }

    private static boolean containsDuplicateIds(List<Long> ids) {
        return new HashSet<>(ids).size() != ids.size();
    }

    private static void requireValidGenerationMetadata(String modelName, String version, long durationMillis,
                                                       long inputTokens, long outputTokens) {
        if (!isValidText(modelName, MAX_MODEL_NAME_LENGTH) || !SUPPORTED_GENERATOR_VERSION.equals(version)
                || !hasNonNegativeUsage(durationMillis, inputTokens, outputTokens)) {
            throw invalidResult();
        }
    }

    private static boolean hasNonNegativeUsage(long durationMillis, long inputTokens, long outputTokens) {
        return durationMillis >= 0 && inputTokens >= 0 && outputTokens >= 0;
    }

    private static boolean isPositiveId(Long id) {
        return id != null && id > 0;
    }

    private static boolean isValidText(String text, int maxLength) {
        return text != null && !text.isBlank() && text.length() <= maxLength;
    }

    private static IllegalArgumentException invalidResult() {
        return new IllegalArgumentException("invalid follow-up result");
    }
}
