import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

const api = vi.hoisted(() => ({
  fetchTopics: vi.fn(),
  fetchConcepts: vi.fn(),
  fetchAdminQuestions: vi.fn(),
  fetchAdminQuestion: vi.fn(),
  replaceQuestionConcepts: vi.fn(),
  createAdminQuestion: vi.fn(),
  updateAdminQuestion: vi.fn(),
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

  it("늦은 상세 응답이 새 문제 입력과 생성 대상을 바꾸지 않는다", async () => {
    let resolveDetail!: (value: typeof networkQuestion) => void;
    api.fetchAdminQuestion.mockReturnValue(new Promise<typeof networkQuestion>(resolve => { resolveDetail = resolve; }));
    api.createAdminQuestion.mockResolvedValue({ ...networkQuestion, id: 20, content: "새 문제" });
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await wrapper.findAll("button").find(button => button.text() === "새 문제")!.trigger("click");
    await wrapper.find(".admin-form select").setValue("2");
    await wrapper.findAll("textarea")[0]!.setValue("새 문제");
    await wrapper.findAll("textarea")[1]!.setValue("새 답안");

    resolveDetail(networkQuestion);
    await flushPromises();

    expect(wrapper.find("h2").text()).toBe("새 문제");
    expect(wrapper.findAll("textarea")[0]!.element.value).toBe("새 문제");
    await wrapper.find("form").trigger("submit");
    await flushPromises();
    expect(api.createAdminQuestion).toHaveBeenCalledWith({ topicId: 2, difficulty: "BASIC", content: "새 문제", referenceAnswer: "새 답안" });
    expect(api.updateAdminQuestion).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it("초기 목록 실패 뒤 재시도하면 로딩을 끝내고 목록을 표시한다", async () => {
    api.fetchAdminQuestions.mockRejectedValueOnce(new Error("temporary"));
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    expect(wrapper.text()).toContain("문제 목록을 불러오지 못했습니다.");
    expect(wrapper.text()).not.toContain("문제를 불러오는 중");

    await wrapper.get("button[data-retry='list']").trigger("click");
    await flushPromises();
    expect(wrapper.get("button[data-question-id='10']").text()).toContain("TCP 질문");
  });

  it("선택 항목을 native button과 aria-pressed로 표현한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    const button = wrapper.get("button[data-question-id='10']");
    expect(button.attributes("aria-pressed")).toBe("false");
    await button.trigger("click");
    await flushPromises();
    expect(button.attributes("aria-pressed")).toBe("true");
  });

  it("저장 중 새 문제로 전환해도 완료 응답이 새 입력을 덮지 않는다", async () => {
    let resolveUpdate!: (value: typeof networkQuestion) => void;
    api.updateAdminQuestion.mockReturnValue(new Promise(resolve => { resolveUpdate = resolve; }));
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await flushPromises();
    await wrapper.find("form").trigger("submit");
    await wrapper.findAll("button").find(button => button.text() === "새 문제")!.trigger("click");
    await wrapper.findAll("textarea")[0]!.setValue("새 입력");
    resolveUpdate({ ...networkQuestion, content: "서버 수정본" });
    await flushPromises();

    expect(wrapper.find("h2").text()).toBe("새 문제");
    expect(wrapper.findAll("textarea")[0]!.element.value).toBe("새 입력");
  });

  it("선택한 문제와 같은 Topic의 Concept만 평가 기준 후보로 표시한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await flushPromises();

    const options = wrapper.find('select[aria-label="추가할 Concept"]').findAll("option");
    expect(options.map(option => option.text())).toEqual(["선택", "TCP 신뢰성"]);
    expect(wrapper.text()).not.toContain("Concept 추가\n스레드");
  });

  it("명시적으로 고른 Concept과 기본 평가 정책을 저장한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
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
