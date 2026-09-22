package com.example.crackcs.content.knowledge.chunk.service;

import com.example.crackcs.content.knowledge.domain.ContentChecksum;

import java.util.ArrayList;
import java.util.List;

public final class KnowledgeChunkPolicy {

    private final int maxLength;
    private final int overlap;
    private final String policyVersion;

    public KnowledgeChunkPolicy(int maxLength, int overlap, String policyVersion) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        if (overlap < 0 || overlap >= maxLength) {
            throw new IllegalArgumentException("overlap must be between 0 and maxLength");
        }
        if (policyVersion == null || policyVersion.isBlank()) {
            throw new IllegalArgumentException("policyVersion must not be blank");
        }
        this.maxLength = maxLength;
        this.overlap = overlap;
        this.policyVersion = policyVersion.trim();
    }

    public List<ChunkSlice> split(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        List<ChunkSlice> slices = new ArrayList<>();
        int cursor = 0;
        for (String paragraph : content.split("\\n\\n")) {
            int start = content.indexOf(paragraph, cursor);
            addParagraphSlices(content, start, start + paragraph.length(), slices);
            cursor = start + paragraph.length();
        }
        return List.copyOf(slices);
    }

    public String generationKey(String documentChecksum) {
        if (documentChecksum == null || documentChecksum.isBlank()) {
            throw new IllegalArgumentException("documentChecksum must not be blank");
        }
        return ContentChecksum.sha256(documentChecksum + ":" + policyVersion);
    }

    public String policyVersion() {
        return policyVersion;
    }

    private void addParagraphSlices(
            String source,
            int paragraphStart,
            int paragraphEnd,
            List<ChunkSlice> slices
    ) {
        int start = paragraphStart;
        while (start < paragraphEnd) {
            int end = Math.min(start + maxLength, paragraphEnd);
            slices.add(new ChunkSlice(slices.size(), start, end, source.substring(start, end)));
            if (end == paragraphEnd) {
                return;
            }
            start = end - overlap;
        }
    }
}
