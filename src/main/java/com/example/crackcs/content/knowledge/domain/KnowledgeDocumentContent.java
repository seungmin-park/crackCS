package com.example.crackcs.content.knowledge.domain;

public final class KnowledgeDocumentContent {

    private final String value;
    private final String checksum;

    private KnowledgeDocumentContent(String value, String checksum) {
        this.value = value;
        this.checksum = checksum;
    }

    public static KnowledgeDocumentContent from(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        String normalizedValue = content
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .strip();
        return new KnowledgeDocumentContent(
                normalizedValue,
                ContentChecksum.sha256(normalizedValue)
        );
    }

    public String value() {
        return value;
    }

    public String checksum() {
        return checksum;
    }
}
