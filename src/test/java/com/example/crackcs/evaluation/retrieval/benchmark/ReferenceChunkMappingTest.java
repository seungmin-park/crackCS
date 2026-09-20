package com.example.crackcs.evaluation.retrieval.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ReferenceChunkMappingTest {
    @Test
    @DisplayName("두 근거가 합쳐진 실제 Chunk는 동일한 DB ID에 연결한다")
    void mapsMergedEvidenceToOnePersistedChunk() {
        Map<String, Set<Long>> result = ReferenceChunkMapping.connect(7L, "가나다\n라마바",
                List.of(new ReferenceChunkMapping.Evidence("K1", 0, 3, "가나다"),
                        new ReferenceChunkMapping.Evidence("K2", 4, 7, "라마바")),
                List.of(new ReferenceChunkMapping.StoredChunk(7L, 91L, 0, 7, "가나다\n라마바")));

        assertThat(result).containsEntry("K1", Set.of(91L)).containsEntry("K2", Set.of(91L));
    }

    @Test
    @DisplayName("하나의 근거가 분할되면 이를 덮는 모든 실제 Chunk에 연결한다")
    void mapsSplitEvidenceAcrossChunks() {
        Map<String, Set<Long>> result = ReferenceChunkMapping.connect(7L, "abcdef",
                List.of(new ReferenceChunkMapping.Evidence("K1", 0, 6, "abcdef")),
                List.of(new ReferenceChunkMapping.StoredChunk(7L, 91L, 0, 4, "abcd"),
                        new ReferenceChunkMapping.StoredChunk(7L, 92L, 3, 6, "def")));

        assertThat(result).containsEntry("K1", Set.of(91L, 92L));
    }

    @Test
    @DisplayName("실제 Chunk 사이에 근거를 덮지 못하는 구간이 있으면 연결을 거부한다")
    void rejectsUncoveredEvidence() {
        assertThatIllegalArgumentException().isThrownBy(() -> ReferenceChunkMapping.connect(7L, "abcdef",
                List.of(new ReferenceChunkMapping.Evidence("K1", 0, 6, "abcdef")),
                List.of(new ReferenceChunkMapping.StoredChunk(7L, 91L, 0, 2, "ab"),
                        new ReferenceChunkMapping.StoredChunk(7L, 92L, 3, 6, "def"))));
    }

    @Test
    @DisplayName("원문 위치가 같아도 다른 문서의 Chunk는 연결하지 않는다")
    void rejectsAnotherDocument() {
        assertThatIllegalArgumentException().isThrownBy(() -> ReferenceChunkMapping.connect(7L, "abc",
                List.of(new ReferenceChunkMapping.Evidence("K1", 0, 3, "abc")),
                List.of(new ReferenceChunkMapping.StoredChunk(8L, 91L, 0, 3, "abc"))));
    }

    @Test
    @DisplayName("근거 원문이 위치에 해당하는 내용과 다르면 연결을 거부한다")
    void rejectsChangedEvidence() {
        assertThatIllegalArgumentException().isThrownBy(() -> ReferenceChunkMapping.connect(7L, "abc",
                List.of(new ReferenceChunkMapping.Evidence("K1", 0, 3, "xyz")),
                List.of(new ReferenceChunkMapping.StoredChunk(7L, 91L, 0, 3, "abc"))));
    }

    @Test
    @DisplayName("실제 Chunk 내용이 원문 위치와 다르면 연결을 거부한다")
    void rejectsChangedStoredChunk() {
        assertThatIllegalArgumentException().isThrownBy(() -> ReferenceChunkMapping.connect(7L, "abc",
                List.of(new ReferenceChunkMapping.Evidence("K1", 0, 3, "abc")),
                List.of(new ReferenceChunkMapping.StoredChunk(7L, 91L, 0, 3, "xyz"))));
    }

    @Test
    @DisplayName("중복 근거 키로 기존 연결을 덮어쓰지 않는다")
    void rejectsDuplicateEvidenceKeys() {
        assertThatIllegalArgumentException().isThrownBy(() -> ReferenceChunkMapping.connect(7L, "abc",
                List.of(new ReferenceChunkMapping.Evidence("K1", 0, 3, "abc"),
                        new ReferenceChunkMapping.Evidence("K1", 0, 3, "abc")),
                List.of(new ReferenceChunkMapping.StoredChunk(7L, 91L, 0, 3, "abc"))));
    }
}
