import { presentRequestError, type RequestErrorPresentation } from "@/presentation/requestErrorPresentation";
import { ref, type Ref } from "vue";

import {
  fetchKnowledgeChunks,
  generateKnowledgeChunks,
  type KnowledgeChunk,
  type KnowledgeDocument,
} from "@/api/admin/knowledgeDocuments";
import type { useAdminFeedback } from "@/composables/useAdminFeedback";

export function useKnowledgeDocumentChunks(
  selectedDocument: Ref<KnowledgeDocument | undefined>,
  selectionGeneration: () => number,
  feedback: ReturnType<typeof useAdminFeedback>,
) {
  const chunks = ref<KnowledgeChunk[]>([]);
  const chunkError = ref<RequestErrorPresentation>();
  const chunksLoading = ref(false);
  let chunkRequestGeneration = 0;

  function invalidateRequests(): void {
    chunkRequestGeneration++;
  }

  function selectDocument(document?: KnowledgeDocument): void {
    invalidateRequests();
    chunks.value = [];
    chunkError.value = undefined;
    chunksLoading.value = false;
    if (document) void loadChunks(document.id);
  }

  function isCurrentRequest(documentId: number, selection: number, request: number): boolean {
    return selection === selectionGeneration()
      && request === chunkRequestGeneration
      && selectedDocument.value?.id === documentId;
  }

  async function loadChunks(documentId: number): Promise<void> {
    const selection = selectionGeneration();
    const request = ++chunkRequestGeneration;
    chunksLoading.value = true;
    chunkError.value = undefined;
    try {
      const documentChunks = await fetchKnowledgeChunks(documentId);
      if (isCurrentRequest(documentId, selection, request)) chunks.value = documentChunks;
    } catch (caught) {
      if (isCurrentRequest(documentId, selection, request)) chunkError.value = presentRequestError(caught);
    } finally {
      if (isCurrentRequest(documentId, selection, request)) chunksLoading.value = false;
    }
  }

  async function generateDocumentChunks(): Promise<void> {
    if (!selectedDocument.value) return;
    const selection = selectionGeneration();
    const documentId = selectedDocument.value.id;
    invalidateRequests();
    chunksLoading.value = false;
    chunkError.value = undefined;
    const generation = await feedback.execute(
      () => generateKnowledgeChunks(documentId),
      "검색용 문단을 생성했습니다. 같은 문서와 정책이면 기존 결과를 재사용합니다.",
    );
    if (generation && selection === selectionGeneration() && selectedDocument.value?.id === documentId) {
      invalidateRequests();
      chunks.value = generation.chunks;
      chunkError.value = undefined;
      chunksLoading.value = false;
    }
  }

  return { chunks, chunkError, chunksLoading, selectDocument, loadChunks, generateDocumentChunks, invalidateRequests };
}
