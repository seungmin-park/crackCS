import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { reactive } from "vue";

import QuestionListView from "@/views/QuestionListView.vue";

const { fetchQuestions } = vi.hoisted(() => ({ fetchQuestions: vi.fn() }));
enableAutoUnmount(afterEach);
const route = reactive<{ query: Record<string, string | undefined> }>({ query: {} });
const push = vi.fn();
vi.mock("vue-router", async (importOriginal) => ({
  ...await importOriginal<typeof import("vue-router")>(),
  useRoute: () => route,
  useRouter: () => ({ push }),
}));

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
    route.query = {};
    push.mockReset();
    push.mockImplementation(({ query }) => { route.query = query; return Promise.resolve(); });
  });

  it("주소에 저장된 난이도와 페이지를 복원한다", async () => {
    route.query = { page: "2", difficulty: "INTERMEDIATE" };
    fetchQuestions.mockResolvedValue({ content: [], page: 1, size: 20, totalElements: 21, totalPages: 2 });
    mountView();
    await flushPromises();
    expect(fetchQuestions).toHaveBeenCalledWith({ page: 1, difficulty: "INTERMEDIATE" });
  });

  it("난이도 선택을 뒤로 가기로 복원할 수 있도록 주소에 기록한다", async () => {
    fetchQuestions.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    const wrapper = mountView();
    await flushPromises();
    await wrapper.get('[data-filter="BASIC"]').trigger("click");
    expect(push).toHaveBeenCalledWith({ query: { page: "1", difficulty: "BASIC" } });
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
    expect(wrapper.get(".state-panel button").text()).toBe("다시 불러오기");
  });

  it("현재 페이지 건수가 아닌 전체 건수를 표시하고 다음 페이지를 조회한다", async () => {
    fetchQuestions.mockResolvedValue({ content: [{ id: 1, topic: { id: 1, name: "운영체제" }, difficulty: "BASIC", content: "프로세스란?" }], page: 0, size: 20, totalElements: 21, totalPages: 2 });
    const wrapper = mountView();
    await flushPromises();
    expect(wrapper.text()).toContain("전체 21문제");
    fetchQuestions.mockResolvedValue({ content: [], page: 1, size: 20, totalElements: 21, totalPages: 2 });
    await wrapper.get('button[aria-label="다음 페이지"]').trigger("click");
    expect(fetchQuestions).toHaveBeenLastCalledWith({ page: 1, difficulty: undefined });
  });

  it("다음 페이지에서 난이도를 바꾸면 첫 페이지를 조회한다", async () => {
    fetchQuestions.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 21, totalPages: 2 });
    const wrapper = mountView();
    await flushPromises();
    fetchQuestions.mockResolvedValue({ content: [], page: 1, size: 20, totalElements: 21, totalPages: 2 });
    await wrapper.get('button[aria-label="다음 페이지"]').trigger("click");
    await flushPromises();
    await wrapper.get('[data-filter="BASIC"]').trigger("click");
    expect(fetchQuestions).toHaveBeenLastCalledWith({ page: 0, difficulty: "BASIC" });
  });

  it("필터 결과가 없으면 전체 공개 문제 부족과 구분해서 안내한다", async () => {
    fetchQuestions.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    const wrapper = mountView();
    await flushPromises();
    await wrapper.get('[data-filter="ADVANCED"]').trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("이 난이도에는 아직 문제가 없어요");
  });

  it("늦게 도착한 이전 조회가 새 필터 결과를 덮어쓰지 않는다", async () => {
    let resolveOld!: (value: unknown) => void;
    fetchQuestions.mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve; }));
    const wrapper = mountView();
    fetchQuestions.mockResolvedValueOnce({ content: [{ id: 2, topic: { id: 1, name: "Java" }, difficulty: "BASIC", content: "새 필터 문제" }], page: 0, size: 20, totalElements: 1, totalPages: 1 });
    await wrapper.get('[data-filter="BASIC"]').trigger("click");
    await flushPromises();
    resolveOld({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    await flushPromises();
    expect(wrapper.text()).toContain("새 필터 문제");
  });
});
