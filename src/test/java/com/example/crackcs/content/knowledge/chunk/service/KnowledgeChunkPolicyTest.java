package com.example.crackcs.content.knowledge.chunk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkPolicyTest {

    private final KnowledgeChunkPolicy policy = new KnowledgeChunkPolicy(
            20,
            5,
            "paragraph-20-overlap-5-v1"
    );

    @Test
    @DisplayName("문단 경계를 유지하며 원문 순서대로 Chunk를 만든다")
    void splitsAtParagraphBoundariesInSourceOrder() {
        String content = "첫 번째 문단이다.\n\n두 번째 문단이다.\n\n세 번째 문단이다.";

        List<ChunkSlice> chunks = policy.split(content);

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

        List<ChunkSlice> chunks = policy.split(content);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).content()).isEqualTo("01234567890123456789");
        assertThat(chunks.get(1).startOffset()).isEqualTo(15);
        assertThat(content.substring(chunks.get(1).startOffset(), chunks.get(1).endOffset()))
                .isEqualTo(chunks.get(1).content());
    }

    @Test
    @DisplayName("같은 문서 checksum과 정책은 같은 generation key를 만든다")
    void createsStableGenerationKey() {
        KnowledgeChunkPolicy policyV1 = new KnowledgeChunkPolicy(20, 5, "policy-v1");

        String generationKey = policyV1.generationKey("document-checksum");

        assertThat(generationKey)
                .isEqualTo("efb97f1cd86fc897d085c65050c8ac3463b724a8bb0c8a854dea203e4bdd8b05");
    }

    @Test
    @DisplayName("정책 버전이 바뀌면 새 generation key를 만든다")
    void changesGenerationKeyWhenPolicyVersionChanges() {
        KnowledgeChunkPolicy policyV2 = new KnowledgeChunkPolicy(20, 5, "policy-v2");

        String generationKey = policyV2.generationKey("document-checksum");

        assertThat(generationKey)
                .isEqualTo("34800e66e5c9453d33853e3ec72d6c03d822c82c29daa65d3bfb4fd873ab8da0");
    }
}
