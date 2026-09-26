import {
  onBeforeUnmount,
  reactive,
  ref,
  type Ref
} from "vue";
import {
  createAdminQuestion,
  createQuestionVersion,
  fetchAdminQuestion,
  publishQuestion,
  retireQuestion,
  reviewQuestion,
  updateAdminQuestion,
  type AdminQuestion,
  type AdminQuestionInput,
  type AdminQuestionSummary,
  type QuestionDifficulty,
} from "@/api/admin/questions";
import type {
  Concept
} from "@/api/admin/concepts";
import {
  useAdminFeedback
} from "@/composables/useAdminFeedback";
import { useQuestionCriteriaEditor } from "@/composables/useQuestionCriteriaEditor";

type QuestionForm = {
  topicId: string;
  difficulty: QuestionDifficulty;
  content: string;
  referenceAnswer: string;
};

function questionForm(question?: AdminQuestion): QuestionForm {
  return question ? {
    topicId: String(question.topicId),
    difficulty: question.difficulty,
    content: question.content,
    referenceAnswer: question.referenceAnswer
  } : {
    topicId: "",
    difficulty: "BASIC",
    content: "",
    referenceAnswer: ""
  };
}

function questionInput(form: QuestionForm): AdminQuestionInput {
  return {
    topicId: Number(form.topicId),
    difficulty: form.difficulty,
    content: form.content,
    referenceAnswer: form.referenceAnswer
  };
}

/** refreshList reloads list data only; filter changes reset selection explicitly in the view. */
export function useAdminQuestionEditor(
  concepts: Ref<Concept[]>,
  refreshList: () => Promise<void>,
) {
  const selectedQuestion = ref<AdminQuestion>();
  let selectionGeneration = 0;
  let disposed = false;
  const detailError = ref(false);
  const feedback = useAdminFeedback();
  const form = reactive(questionForm());
  const criteriaEditor = useQuestionCriteriaEditor(
    concepts, () => form.topicId, selectedQuestion, () => selectionGeneration, feedback,
  );

  function clearSelection() {
    selectionGeneration++;
    selectedQuestion.value = undefined;
    Object.assign(form, questionForm());
    criteriaEditor.setCriteria();
    detailError.value = false;
  }

  async function selectQuestionForEditing(question: AdminQuestionSummary) {
    const activeGeneration = ++selectionGeneration;
    detailError.value = false;
    try {
      const detail = await fetchAdminQuestion(question.id);
      if (activeGeneration !== selectionGeneration) return;
      selectedQuestion.value = detail;
      Object.assign(form, questionForm(detail));
      criteriaEditor.setCriteria(detail);
    } catch {
      if (activeGeneration === selectionGeneration) detailError.value = true;
    }
  }

  async function saveQuestionDraft() {
    const activeGeneration = selectionGeneration;
    const target = selectedQuestion.value ? {
      id: selectedQuestion.value.id,
      status: selectedQuestion.value.status
    } : undefined;
    const payload = questionInput(form);
    const result = await feedback.execute(
      () => target?.status === "DRAFT" ? updateAdminQuestion(target.id, payload) :
        createAdminQuestion(payload),
      target?.status === "DRAFT" ? "문제 초안을 수정했습니다." : "문제 초안을 등록했습니다.",
    );
    if (result && activeGeneration === selectionGeneration) selectedQuestion.value = result;
    if (result && !disposed) await refreshList();
  }

  async function changeQuestionStatus(
    action: (questionId: number) => Promise<AdminQuestion>, message: string,
  ): Promise<void> {
    if (!selectedQuestion.value) return;
    const activeGeneration = selectionGeneration;
    const questionId = selectedQuestion.value.id;
    const result = await feedback.execute(() => action(questionId), message);
    if (result && activeGeneration === selectionGeneration) selectedQuestion.value = result;
    if (result && !disposed) await refreshList();
  }

  function reviewSelectedQuestion(): Promise<void> {
    return changeQuestionStatus(reviewQuestion, "문제 검수를 기록했습니다.");
  }

  function publishSelectedQuestion(): Promise<void> {
    return changeQuestionStatus(publishQuestion, "문제를 공개했습니다.");
  }

  function retireSelectedQuestion(): Promise<void> {
    return changeQuestionStatus(retireQuestion, "문제를 폐기했습니다.");
  }

  async function newVersion() {
    if (!selectedQuestion.value) return;
    const activeGeneration = selectionGeneration;
    const targetId = selectedQuestion.value.id;
    const payload = {
      difficulty: form.difficulty,
      content: form.content,
      referenceAnswer: form.referenceAnswer
    };
    const result = await feedback.execute(
      () => createQuestionVersion(targetId, payload),
      "문제의 새 DRAFT 버전을 만들고 평가 Concept을 복사했습니다.",
    );
    if (result && activeGeneration === selectionGeneration) {
      selectedQuestion.value = result;
      criteriaEditor.setCriteria(result);
    }
    if (result && !disposed) await refreshList();
  }

  onBeforeUnmount(() => {
    disposed = true;
    selectionGeneration++;
  });
  return {
    selectedQuestion,
    detailError,
    feedback,
    form,
    ...criteriaEditor,
    clearSelection,
    selectQuestionForEditing,
    saveQuestionDraft,
    reviewSelectedQuestion,
    publishSelectedQuestion,
    retireSelectedQuestion,
    newVersion
  };
}
