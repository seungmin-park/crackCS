import { describe, expect, it } from "vitest";
import { ref } from "vue";

import { useAdminFeedback } from "@/composables/useAdminFeedback";
import { useKnowledgeDocumentChunks } from "@/composables/useKnowledgeDocumentChunks";

describe("문서 검색 문단 편집", () => {
  it("문서 선택을 해제하면 검색 문단 상태도 비운다", () => {
    const chunks = useKnowledgeDocumentChunks(ref(), () => 0, useAdminFeedback());

    chunks.selectDocument(undefined);

    expect(chunks.chunks.value).toEqual([]);
    expect(chunks.chunkError.value).toBeUndefined();
    expect(chunks.chunksLoading.value).toBe(false);
  });
});
