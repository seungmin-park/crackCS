package com.example.crackcs.content.knowledge.chunk.controller;

import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunk;
import com.example.crackcs.content.knowledge.chunk.domain.KnowledgeChunkSearchStatus;
import com.example.crackcs.content.knowledge.chunk.service.ChunkGenerationResult;
import com.example.crackcs.content.knowledge.chunk.service.KnowledgeChunkService;
import com.example.crackcs.content.knowledge.controller.KnowledgeDocumentController;
import com.example.crackcs.content.knowledge.service.KnowledgeDocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class KnowledgeChunkControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    KnowledgeDocumentService documentService;
    @MockitoBean
    KnowledgeChunkService chunkService;

    @Test
    @DisplayName("Chunk를 동기로 생성하면 작업 키와 생성된 Chunk 상태를 반환한다")
    void generatesChunksSynchronously() throws Exception {
        KnowledgeChunk chunk = chunk(7L, 0, "프로세스 설명");
        given(chunkService.generateChunks(3L))
                .willReturn(new ChunkGenerationResult("generation-key", false, List.of(chunk)));

        mockMvc.perform(post("/api/admin/knowledge-documents/{documentId}/chunks", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationKey").value("generation-key"))
                .andExpect(jsonPath("$.reused").value(false))
                .andExpect(jsonPath("$.chunks[0].id").value(7L))
                .andExpect(jsonPath("$.chunks[0].searchStatus").value("KEYWORD_SEARCHABLE"));
    }

    @Test
    @DisplayName("문서의 Chunk 목록은 원문 순서와 인용 범위를 반환한다")
    void findsChunksInSourceOrder() throws Exception {
        KnowledgeChunk chunk = chunk(8L, 1, "스레드 설명");
        given(chunkService.findByDocumentId(3L)).willReturn(List.of(chunk));

        mockMvc.perform(get("/api/admin/knowledge-documents/{documentId}/chunks", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sequenceNo").value(1))
                .andExpect(jsonPath("$[0].startOffset").value(10))
                .andExpect(jsonPath("$[0].endOffset").value(16))
                .andExpect(jsonPath("$[0].content").value("스레드 설명"));
    }

    private KnowledgeChunk chunk(Long id, int sequence, String content) {
        KnowledgeChunk chunk = mock(KnowledgeChunk.class);
        given(chunk.getId()).willReturn(id);
        given(chunk.getSequenceNo()).willReturn(sequence);
        given(chunk.getStartOffset()).willReturn(10);
        given(chunk.getEndOffset()).willReturn(16);
        given(chunk.getContent()).willReturn(content);
        given(chunk.getChecksum()).willReturn("checksum");
        given(chunk.getGenerationKey()).willReturn("generation-key");
        given(chunk.getSearchStatus()).willReturn(KnowledgeChunkSearchStatus.KEYWORD_SEARCHABLE);
        return chunk;
    }
}
