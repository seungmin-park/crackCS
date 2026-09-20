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

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public String searchText() {
        // The submitted answer is untrusted: a fluent wrong answer must not choose its own evidence.
        return String.join(" ", safe(question), safe(referenceAnswer));
    }
}
