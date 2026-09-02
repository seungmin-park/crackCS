package com.example.crackcs.content.knowledge.domain;

public enum KnowledgeSourceType {
    OFFICIAL_SPEC(true),
    OFFICIAL_DOC(true),
    INTERNAL_SUMMARY(false);

    private final boolean sourceUrlRequired;

    KnowledgeSourceType(boolean sourceUrlRequired) {
        this.sourceUrlRequired = sourceUrlRequired;
    }

    public boolean isSourceUrlRequired() {
        return sourceUrlRequired;
    }
}
