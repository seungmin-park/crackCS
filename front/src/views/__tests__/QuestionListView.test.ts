import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

import QuestionListView from "@/views/QuestionListView.vue";

const { fetchQuestions } = vi.hoisted(() => ({ fetchQuestions: vi.fn() }));

vi.mock("@/api/questions", async (importOriginal) => {
  const original = await importOriginal<typeof import("@/api/questions")>();
  return { ...original, fetchQuestions };
});

function mountView() {
  return mount(QuestionListView, {
    global: {
      stubs: {
        RouterLink: { props: ["to"], template: "<a :href='to'><slot /></a>" },
      },
    },
  });
}

describe("문제 목록 화면", () => {
  beforeEach(() => {
    fetchQuestions.mockReset();
  });

  it("조회 중에는 로딩 상태를 표시한다", () => {
    fetchQuestions.mockReturnValue(new Promise(() => undefined));

    const wrapper = mountView();

    expect(wrapper.find("[aria-label='문제 목록을 불러오는 중']").exists()).toBe(true);
  });

  it("공개 문제의 Topic과 난이도와 본문을 표시한다", async () => {
    fetchQuestions.mockResolvedValue({
      content: [{ id: 7, topic: { id: 1, code: "OS", name: "운영체제" }, difficulty: "BASIC", content: "프로세스란 무엇인가요?" }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain("운영체제");
    expect(wrapper.text()).toContain("기본");
    expect(wrapper.text()).toContain("프로세스란 무엇인가요?");
    expect(wrapper.html()).not.toContain("referenceAnswer");
    expect(wrapper.html()).not.toContain("weight");
  });

  it("공개 문제가 없으면 빈 상태를 표시한다", async () => {
    fetchQuestions.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain("아직 공개된 문제가 없어요");
  });

  it("서버 오류가 발생하면 재시도 상태를 표시한다", async () => {
    fetchQuestions.mockRejectedValue(new Error("server error"));

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain("문제를 불러오지 못했어요");
    expect(wrapper.get("button").text()).toBe("다시 불러오기");
  });
});
