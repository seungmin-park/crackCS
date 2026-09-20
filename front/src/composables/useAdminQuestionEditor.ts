import {
  computed,
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
  replaceQuestionConcepts,
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

type QuestionForm = {
  topicId: string;
  difficulty: QuestionDifficulty;
  content: string;
  referenceAnswer: string;
};

type QuestionCriterion = {
  conceptId: number;
  weight: number;
  required: boolean;
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

function questionCriteria(question: AdminQuestion): QuestionCriterion[] {
  return question.concepts.map(({
    conceptId,
    weight,
    required
  }) => ({
    conceptId,
    weight,
    required
  }));
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
  const selected = ref<AdminQuestion>();
  let selectionGeneration = 0;
  let disposed = false;
  const detailError = ref(false);
  const feedback = useAdminFeedback();
  const form = reactive(questionForm());
  const criteria = ref<QuestionCriterion[]>([]);
  const newCriterionConceptId = ref<number>();
  const topicConcepts = computed(() => concepts.value.filter(
    item => item.topicId === Number(form.topicId),
  ));
  const availableConcepts = computed(() => topicConcepts.value.filter(
    item => !criteria.value.some(row => row.conceptId === item.id),
  ));

  function clearSelection() {
    selectionGeneration++;
    selected.value = undefined;
    Object.assign(form, questionForm());
    criteria.value = [];
    detailError.value = false;
  }

  async function select(question: AdminQuestionSummary) {
    const activeGeneration = ++selectionGeneration;
    detailError.value = false;
    try {
      const detail = await fetchAdminQuestion(question.id);
      if (activeGeneration !== selectionGeneration) return;
      selected.value = detail;
      Object.assign(form, questionForm(detail));
      criteria.value = questionCriteria(detail);
    } catch {
      if (activeGeneration === selectionGeneration) detailError.value = true;
    }
  }

  async function submit() {
    const activeGeneration = selectionGeneration;
    const target = selected.value ? {
      id: selected.value.id,
      status: selected.value.status
    } : undefined;
    const payload = questionInput(form);
    const result = await feedback.execute(
      () => target?.status === "DRAFT" ? updateAdminQuestion(target.id, payload) :
        createAdminQuestion(payload),
      target?.status === "DRAFT" ? "문제 초안을 수정했습니다." : "문제 초안을 등록했습니다.",
    );
    if (result && activeGeneration === selectionGeneration) selected.value = result;
    if (result && !disposed) await refreshList();
  }

  function addCriterion() {
    const candidate = availableConcepts.value.find(item => item.id === newCriterionConceptId.value);
    if (candidate) {
      criteria.value.push({
        conceptId: candidate.id,
        weight: 1,
        required: true
      });
      newCriterionConceptId.value = undefined;
    }
  }

  async function saveCriteria() {
    if (!selected.value) return;
    const activeGeneration = selectionGeneration;
    const targetId = selected.value.id;
    const payload = criteria.value.map(item => ({
      ...item
    }));
    const result = await feedback.execute(() => replaceQuestionConcepts(targetId, payload),
      "평가 Concept을 교체했습니다.");
    if (result && activeGeneration === selectionGeneration) selected.value = result;
  }

  async function transition(action: "review" | "publish" | "retire") {
    if (!selected.value) return;
    const activeGeneration = selectionGeneration;
    const targetId = selected.value.id;
    const calls = {
      review: reviewQuestion,
      publish: publishQuestion,
      retire: retireQuestion
    };
    const messages = {
      review: "문제 검수를 기록했습니다.",
      publish: "문제를 공개했습니다.",
      retire: "문제를 폐기했습니다."
    };
    const result = await feedback.execute(() => calls[action](targetId), messages[action]);
    if (result && activeGeneration === selectionGeneration) selected.value = result;
    if (result && !disposed) await refreshList();
  }

  async function newVersion() {
    if (!selected.value) return;
    const activeGeneration = selectionGeneration;
    const targetId = selected.value.id;
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
      selected.value = result;
      criteria.value = questionCriteria(result);
    }
    if (result && !disposed) await refreshList();
  }

  onBeforeUnmount(() => {
    disposed = true;
    selectionGeneration++;
  });
  return {
    selected,
    detailError,
    feedback,
    form,
    criteria,
    newCriterionConceptId,
    topicConcepts,
    availableConcepts,
    clearSelection,
    select,
    submit,
    addCriterion,
    saveCriteria,
    transition,
    newVersion
  };
}
