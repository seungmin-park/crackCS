import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

const api = vi.hoisted(() => ({
  fetchTopics: vi.fn(),
  fetchConcepts: vi.fn(),
  fetchAdminQuestions: vi.fn(),
  fetchAdminQuestion: vi.fn(),
  replaceQuestionConcepts: vi.fn(),
}));

vi.mock("@/api/admin", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/api/admin")>()),
  ...api,
}));

import AdminQuestionView from "@/views/admin/AdminQuestionView.vue";

const networkQuestion = {
  id: 10,
  topicId: 2,
  origin: "ADMIN" as const,
  type: "NORMAL" as const,
  difficulty: "BASIC" as const,
  content: "TCP 질문",
  referenceAnswer: "TCP 답안",
  status: "DRAFT" as const,
  versionSeriesId: "series-1",
  questionVersion: 1,
  createdByMemberId: 1,
  reviewedByMemberId: null,
  reviewedAt: null,
  concepts: [],
  createdAt: "2026-09-01T00:00:00",
  updatedAt: "2026-09-01T00:00:00",
};

describe("관리자 Question 화면", () => {
  beforeEach(() => {
    Object.values(api).forEach(mock => mock.mockReset());
    api.fetchTopics.mockResolvedValue({
      content: [
        { id: 1, parentId: null, code: "OS", name: "운영체제", active: true },
        { id: 2, parentId: null, code: "NETWORK", name: "네트워크", active: true },
      ],
      page: 0, size: 100, totalElements: 2, totalPages: 1,
    });
    api.fetchConcepts.mockResolvedValue({
      content: [
        { id: 11, topicId: 1, code: "THREAD", name: "스레드", description: null, active: true },
        { id: 12, topicId: 2, code: "TCP", name: "TCP 신뢰성", description: null, active: true },
      ],
      page: 0, size: 100, totalElements: 2, totalPages: 1,
    });
    api.fetchAdminQuestions.mockResolvedValue({
      content: [networkQuestion], page: 0, size: 100, totalElements: 1, totalPages: 1,
    });
    api.fetchAdminQuestion.mockResolvedValue(networkQuestion);
    api.replaceQuestionConcepts.mockResolvedValue({
      ...networkQuestion,
      concepts: [{ conceptId: 12, code: "TCP", name: "TCP 신뢰성", weight: 1, required: true }],
    });
  });

  it("선택한 문제와 같은 Topic의 Concept만 평가 기준 후보로 표시한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.find(".admin-list li").trigger("click");
    await flushPromises();

    const options = wrapper.find('select[aria-label="추가할 Concept"]').findAll("option");
    expect(options.map(option => option.text())).toEqual(["선택", "TCP 신뢰성"]);
    expect(wrapper.text()).not.toContain("Concept 추가\n스레드");
  });

  it("명시적으로 고른 Concept과 기본 평가 정책을 저장한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.find(".admin-list li").trigger("click");
    await flushPromises();

    const addButton = wrapper.findAll("button").find(button => button.text() === "Concept 추가")!;
    expect(addButton.attributes("disabled")).toBeDefined();
    await wrapper.find('select[aria-label="추가할 Concept"]').setValue("12");
    await addButton.trigger("click");
    await wrapper.findAll("button").find(button => button.text() === "평가 기준 저장")!.trigger("click");
    await flushPromises();

    expect(api.replaceQuestionConcepts).toHaveBeenCalledWith(10, [
      { conceptId: 12, weight: 1, required: true },
    ]);
    expect(wrapper.text()).toContain("평가 Concept을 교체했습니다.");
  });
});
