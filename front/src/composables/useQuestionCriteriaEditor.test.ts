import { describe, expect, it } from "vitest";
import { ref } from "vue";

import { useQuestionCriteriaEditor } from "@/composables/useQuestionCriteriaEditor";
import { useAdminFeedback } from "@/composables/useAdminFeedback";

describe("Question 평가 기준 편집", () => {
  it("선택한 문제 Topic의 Concept만 추가 후보로 제공한다", () => {
    const concepts = ref([
      { id: 11, topicId: 1, code: "THREAD", name: "스레드", description: null, active: true },
      { id: 12, topicId: 2, code: "TCP", name: "TCP", description: null, active: true },
    ]);
    const selectedQuestion = ref();
    const editor = useQuestionCriteriaEditor(concepts, () => "2", selectedQuestion, () => 0, useAdminFeedback());

    expect(editor.availableConcepts.value.map(concept => concept.id)).toEqual([12]);
    editor.newCriterionConceptId.value = 12;
    editor.addCriterion();
    expect(editor.availableConcepts.value).toEqual([]);
  });
});
