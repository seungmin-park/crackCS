import { computed, ref, type Ref } from "vue";

import type { Concept } from "@/api/admin/concepts";
import { replaceQuestionConcepts, type AdminQuestion } from "@/api/admin/questions";
import type { useAdminFeedback } from "@/composables/useAdminFeedback";

type QuestionCriterion = { conceptId: number; weight: number; required: boolean };

export function useQuestionCriteriaEditor(
  concepts: Ref<Concept[]>,
  topicId: () => string,
  selectedQuestion: Ref<AdminQuestion | undefined>,
  selectionGeneration: () => number,
  feedback: ReturnType<typeof useAdminFeedback>,
) {
  const criteria = ref<QuestionCriterion[]>([]);
  const newCriterionConceptId = ref<number>();
  const topicConcepts = computed(() => concepts.value.filter(concept => concept.topicId === Number(topicId())));
  const availableConcepts = computed(() => topicConcepts.value.filter(
    concept => !criteria.value.some(criterion => criterion.conceptId === concept.id),
  ));

  function setCriteria(question?: AdminQuestion): void {
    criteria.value = question?.concepts.map(({ conceptId, weight, required }) => ({ conceptId, weight, required })) ?? [];
    newCriterionConceptId.value = undefined;
  }

  function addCriterion(): void {
    const concept = availableConcepts.value.find(candidate => candidate.id === newCriterionConceptId.value);
    if (!concept) return;
    criteria.value.push({ conceptId: concept.id, weight: 1, required: true });
    newCriterionConceptId.value = undefined;
  }

  async function saveCriteria(): Promise<void> {
    if (!selectedQuestion.value) return;
    const activeGeneration = selectionGeneration();
    const questionId = selectedQuestion.value.id;
    const input = criteria.value.map(criterion => ({ ...criterion }));
    const updatedQuestion = await feedback.execute(
      () => replaceQuestionConcepts(questionId, input),
      "평가 Concept을 교체했습니다.",
    );
    if (updatedQuestion && activeGeneration === selectionGeneration()) selectedQuestion.value = updatedQuestion;
  }

  return { criteria, newCriterionConceptId, topicConcepts, availableConcepts, setCriteria, addCriterion, saveCriteria };
}
