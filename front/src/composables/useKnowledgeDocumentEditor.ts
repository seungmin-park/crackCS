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
  type KnowledgeDocument,
  type KnowledgeDocumentInput,
} from "@/api/admin/knowledgeDocuments";
import {
  useAdminFeedback
} from "@/composables/useAdminFeedback";
import { useKnowledgeDocumentChunks } from "@/composables/useKnowledgeDocumentChunks";

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
  const selectedDocument = ref<KnowledgeDocument>();
  const feedback = useAdminFeedback();
  let selectionGeneration = 0;
  let disposed = false;
  const form = reactive(documentForm());
  const chunkEditor = useKnowledgeDocumentChunks(selectedDocument, () => selectionGeneration, feedback);

  function selectDocumentForEditing(document?: KnowledgeDocument) {
    selectionGeneration++;
    selectedDocument.value = document;
    Object.assign(form, documentForm(document));
    chunkEditor.selectDocument(document);
  }

  async function saveDocumentDraft() {
    const generation = selectionGeneration;
    const target = selectedDocument.value ? {
      id: selectedDocument.value.id,
      status: selectedDocument.value.status
    } : undefined;
    const payload = documentInput(form);
    const result = await feedback.execute(
      () => target?.status === "DRAFT" ? updateKnowledgeDocument(target.id, payload) :
        createKnowledgeDocument(payload),
      target?.status === "DRAFT" ? "문서 초안을 수정했습니다. 수정 후에는 다시 검수해야 합니다." : "문서 초안을 등록했습니다.",
    );
    if (result && generation === selectionGeneration) selectDocumentForEditing(result);
    if (result && !disposed) await refreshList();
  }

  async function createVersion() {
    if (!selectedDocument.value) return;
    const generation = selectionGeneration;
    const targetId = selectedDocument.value.id;
    const payload = documentInput(form);
    const result = await feedback.execute(() => createKnowledgeDocumentVersion(targetId, payload),
      "새 DRAFT 버전을 생성했습니다.");
    if (result && generation === selectionGeneration) selectDocumentForEditing(result);
    if (result && !disposed) await refreshList();
  }

  async function changeDocumentStatus(
    action: (documentId: number) => Promise<KnowledgeDocument>, message: string,
  ): Promise<void> {
    if (!selectedDocument.value) return;
    const generation = selectionGeneration;
    const documentId = selectedDocument.value.id;
    const result = await feedback.execute(() => action(documentId), message);
    if (result && generation === selectionGeneration) selectDocumentForEditing(result);
    if (result && !disposed) await refreshList();
  }

  function reviewSelectedDocument(): Promise<void> {
    return changeDocumentStatus(reviewKnowledgeDocument, "문서 검수를 기록했습니다.");
  }

  function publishSelectedDocument(): Promise<void> {
    return changeDocumentStatus(publishKnowledgeDocument, "문서를 공개했습니다.");
  }

  function retireSelectedDocument(): Promise<void> {
    return changeDocumentStatus(retireKnowledgeDocument, "문서를 폐기 상태로 전환했습니다.");
  }

  onBeforeUnmount(() => {
    disposed = true;
    selectionGeneration++;
    chunkEditor.invalidateRequests();
  });
  return {
    selectedDocument,
    feedback,
    ...chunkEditor,
    form,
    selectDocumentForEditing,
    saveDocumentDraft,
    createVersion,
    reviewSelectedDocument,
    publishSelectedDocument,
    retireSelectedDocument,
  };
}
