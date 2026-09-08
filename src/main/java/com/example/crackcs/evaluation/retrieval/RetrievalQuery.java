package com.example.crackcs.evaluation.retrieval;

import java.util.List;

public record RetrievalQuery(
        Long topicId,
        List<String> conceptNames,
        String question,
        String referenceAnswer,
        String answer
) {
    public RetrievalQuery {
        if (topicId == null) {
            throw new IllegalArgumentException("topicId must not be null");
        }
        conceptNames = conceptNames == null ? List.of() : List.copyOf(conceptNames);
    }

    public String searchText() {
        return String.join(" ", safe(question), safe(referenceAnswer), safe(answer));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
