package com.example.crackcs.content.knowledge.chunk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkerTest {

    private final KnowledgeChunker chunker = new KnowledgeChunker(20, 5);

    @Test
    @DisplayName("문단 경계를 유지하며 원문 순서대로 Chunk를 만든다")
    void splitsAtParagraphBoundariesInSourceOrder() {
        String content = "첫 번째 문단이다.\n\n두 번째 문단이다.\n\n세 번째 문단이다.";

        List<ChunkSlice> chunks = chunker.split(content);

        assertThat(chunks).extracting(ChunkSlice::content)
                .containsExactly("첫 번째 문단이다.", "두 번째 문단이다.", "세 번째 문단이다.");
        assertThat(chunks).extracting(ChunkSlice::sequenceNo).containsExactly(0, 1, 2);
        assertThat(chunks).allSatisfy(slice ->
                assertThat(content.substring(slice.startOffset(), slice.endOffset())).isEqualTo(slice.content()));
    }

    @Test
    @DisplayName("긴 문단은 overlap을 포함하되 원문 offset으로 복원할 수 있게 나눈다")
    void splitsLongParagraphWithOverlap() {
        String content = "012345678901234567890123456789";

        List<ChunkSlice> chunks = chunker.split(content);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).content()).isEqualTo("01234567890123456789");
        assertThat(chunks.get(1).startOffset()).isEqualTo(15);
        assertThat(content.substring(chunks.get(1).startOffset(), chunks.get(1).endOffset()))
                .isEqualTo(chunks.get(1).content());
    }
}
