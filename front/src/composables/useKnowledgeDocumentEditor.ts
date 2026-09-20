import {
  onBeforeUnmount,
  reactive,
  ref
} from "vue";
import {
  createKnowledgeDocument,
  createKnowledgeDocumentVersion,
  publishKnowledgeDocument,
  retireKnowledgeDocument,
  reviewKnowledgeDocument,
  updateKnowledgeDocument,
  fetchKnowledgeChunks,
  generateKnowledgeChunks,
  type KnowledgeDocument,
  type KnowledgeDocumentInput,
  type KnowledgeChunk,
} from "@/api/admin/knowledgeDocuments";
import {
  useAdminFeedback
} from "@/composables/useAdminFeedback";

type DocumentForm = Omit<KnowledgeDocumentInput, "topicId"> & {
  topicId: string;
};

function documentForm(document?: KnowledgeDocument): DocumentForm {
  return document ? {
    topicId: String(document.topicId),
    title: document.title,
    sourceType: document.sourceType,
    sourceUrl: document.sourceUrl ?? "",
    technologyVersion: document.technologyVersion ?? "",
    licenseNote: document.licenseNote ?? "",
    content: document.content,
  } : {
    topicId: "",
    title: "",
    sourceType: "OFFICIAL_DOC",
    sourceUrl: "",
    technologyVersion: "",
    licenseNote: "",
    content: ""
  };
}

function documentInput(form: DocumentForm): KnowledgeDocumentInput {
  return {
    ...form,
    topicId: Number(form.topicId)
  };
}

/** refreshList reloads list data only; filter changes reset selection explicitly in the view. */
export function useKnowledgeDocumentEditor(refreshList: () => Promise<void>) {
  const selected = ref<KnowledgeDocument>();
  const feedback = useAdminFeedback();
  const chunks = ref<KnowledgeChunk[]>([]);
  const chunkError = ref(false);
  const chunksLoading = ref(false);
  let selectionGeneration = 0;
  let chunkRequestGeneration = 0;
  const form = reactive(documentForm());

  function select(document?: KnowledgeDocument) {
    selectionGeneration++;
    chunkRequestGeneration++;
    selected.value = document;
    Object.assign(form, documentForm(document));
    chunks.value = [];
    chunkError.value = false;
    chunksLoading.value = false;
    if (document) void loadChunks(document.id);
  }

  function isCurrentChunkRequest(documentId: number, selection: number, request: number) {
    return selection === selectionGeneration
      && request === chunkRequestGeneration
      && selected.value?.id === documentId;
  }

  async function loadChunks(documentId: number) {
    const selection = selectionGeneration;
    const request = ++chunkRequestGeneration;
    chunksLoading.value = true;
    chunkError.value = false;
    try {
      const result = await fetchKnowledgeChunks(documentId);
      if (isCurrentChunkRequest(documentId, selection, request)) chunks.value = result;
    } catch {
      if (isCurrentChunkRequest(documentId, selection, request)) chunkError.value = true;
    } finally {
      if (isCurrentChunkRequest(documentId, selection, request)) chunksLoading.value = false;
    }
  }

  async function chunkDocument() {
    if (!selected.value) return;
    const generation = selectionGeneration;
    const targetId = selected.value.id;
    chunkRequestGeneration++;
    chunksLoading.value = false;
    chunkError.value = false;
    const result = await feedback.execute(
      () => generateKnowledgeChunks(targetId),
      "검색용 문단을 생성했습니다. 같은 문서와 정책이면 기존 결과를 재사용합니다.",
    );
    if (result && generation === selectionGeneration && selected.value?.id === targetId) {
      chunkRequestGeneration++;
      chunks.value = result.chunks;
      chunkError.value = false;
      chunksLoading.value = false;
    }
  }

  async function submit() {
    const generation = selectionGeneration;
    const target = selected.value ? {
      id: selected.value.id,
      status: selected.value.status
    } : undefined;
    const payload = documentInput(form);
    const result = await feedback.execute(
      () => target?.status === "DRAFT" ? updateKnowledgeDocument(target.id, payload) :
        createKnowledgeDocument(payload),
      target?.status === "DRAFT" ? "문서 초안을 수정했습니다. 수정 후에는 다시 검수해야 합니다." : "문서 초안을 등록했습니다.",
    );
    if (result && generation === selectionGeneration) select(result);
    if (result) await refreshList();
  }

  async function createVersion() {
    if (!selected.value) return;
    const generation = selectionGeneration;
    const targetId = selected.value.id;
    const payload = documentInput(form);
    const result = await feedback.execute(() => createKnowledgeDocumentVersion(targetId, payload),
      "새 DRAFT 버전을 생성했습니다.");
    if (result && generation === selectionGeneration) select(result);
    if (result) await refreshList();
  }

  async function transition(action: "review" | "publish" | "retire") {
    if (!selected.value) return;
    const generation = selectionGeneration;
    const targetId = selected.value.id;
    const calls = {
      review: reviewKnowledgeDocument,
      publish: publishKnowledgeDocument,
      retire: retireKnowledgeDocument
    };
    const messages = {
      review: "문서 검수를 기록했습니다.",
      publish: "문서를 공개했습니다.",
      retire: "문서를 폐기 상태로 전환했습니다."
    };
    const result = await feedback.execute(() => calls[action](targetId), messages[action]);
    if (result && generation === selectionGeneration) select(result);
    if (result) await refreshList();
  }

  onBeforeUnmount(() => {
    selectionGeneration++;
    chunkRequestGeneration++;
  });
  return {
    selected,
    feedback,
    chunks,
    chunkError,
    chunksLoading,
    form,
    select,
    loadChunks,
    chunkDocument,
    submit,
    createVersion,
    transition
  };
}
