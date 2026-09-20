import { reactive } from "vue";
import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import AnswerHistoryView from "@/views/AnswerHistoryView.vue";

const { fetchMyAnswers } = vi.hoisted(() => ({ fetchMyAnswers: vi.fn() }));
vi.mock("@/api/answers", () => ({ fetchMyAnswers }));
const route = reactive({ query: {} as Record<string, string> });
const push = vi.fn(async (location: { query: Record<string, string> }) => { route.query = location.query; });
const replace = vi.fn(async (location: { query: Record<string, string> }) => { route.query = location.query; });
vi.mock("vue-router", () => ({ useRoute: () => route, useRouter: () => ({ push, replace }) }));

enableAutoUnmount(afterEach);

describe("답변 이력 화면", () => {
  beforeEach(() => { fetchMyAnswers.mockReset(); route.query = {}; push.mockClear(); replace.mockClear(); });

  it("최신 답변의 질문과 평가 상태를 상세 링크로 표시한다", async () => {
    fetchMyAnswers.mockResolvedValue({
      content: [{ answerId: 31, questionId: 7, questionContent: "프로세스와 스레드의 차이는?", content: "답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: { status: "EVALUATED", verdict: "PARTIALLY_CORRECT", score: 50, feedback: "보완", failureReason: null, concepts: [] } }],
      page: 0, size: 20, totalElements: 1, totalPages: 1,
    });
    const wrapper = mount(AnswerHistoryView, { global: { stubs: { RouterLink: { props: ["to"], template: "<a :data-to='JSON.stringify(to)'><slot /></a>" } } } });
    await flushPromises();

    expect(wrapper.text()).toContain("프로세스와 스레드의 차이는?");
    expect(wrapper.text()).toContain("부분 정답");
    expect(wrapper.get("a").attributes("data-to")).toContain("answer-detail");
  });

  it("처리 중인 평가는 결과 없음이 아니라 평가 중으로 표시한다", async () => {
    fetchMyAnswers.mockResolvedValue({
      content: [{
        answerId: 32,
        questionId: 8,
        questionContent: "DNS 캐시는 왜 필요한가요?",
        content: "답변",
        submittedAt: "2026-09-07T10:00:00Z",
        evaluation: {
          status: "PROCESSING",
          verdict: null,
          score: null,
          feedback: null,
          failureReason: null,
          concepts: [],
        },
      }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    const wrapper = mount(AnswerHistoryView, {
      global: {
        stubs: {
          RouterLink: {
            props: ["to"],
            template: "<a><slot /></a>",
          },
        },
      },
    });
    await flushPromises();

    expect(wrapper.get(".evaluation-badge").text()).toBe("평가 중");
    expect(wrapper.text()).not.toContain("평가 결과 없음");
  });

  it("다음 페이지로 이동하면 이전 답변 이력을 불러온다", async () => {
    fetchMyAnswers.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 21, totalPages: 2 });
    const wrapper = mount(AnswerHistoryView, { global: { stubs: { RouterLink: true } } });
    await flushPromises();
    await wrapper.get("button[aria-label='다음 페이지']").trigger("click");
    await flushPromises();
    expect(fetchMyAnswers).toHaveBeenLastCalledWith({ page: 1, size: 20 });
    wrapper.unmount();
  });

  it("범위 밖 페이지는 다른 query를 보존한 마지막 0-based 페이지로 교정하고 한 번 다시 조회한다", async () => {
    route.query = { page: "8", source: "profile" };
    fetchMyAnswers
      .mockResolvedValueOnce({ content: [], page: 8, size: 20, totalElements: 41, totalPages: 3 })
      .mockResolvedValueOnce({ content: [], page: 2, size: 20, totalElements: 41, totalPages: 3 });

    mount(AnswerHistoryView, { global: { stubs: { RouterLink: true } } });
    await flushPromises();

    expect(replace).toHaveBeenCalledOnce();
    expect(replace).toHaveBeenCalledWith({ query: { page: "2", source: "profile" } });
    expect(fetchMyAnswers).toHaveBeenNthCalledWith(1, { page: 8, size: 20 });
    expect(fetchMyAnswers).toHaveBeenNthCalledWith(2, { page: 2, size: 20 });
    expect(fetchMyAnswers).toHaveBeenCalledTimes(2);
  });

  it("빈 결과의 범위 밖 페이지는 0페이지로 교정하고 반복 보정하지 않는다", async () => {
    route.query = { page: "4", source: "profile" };
    fetchMyAnswers.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    mount(AnswerHistoryView, { global: { stubs: { RouterLink: true } } });
    await flushPromises();

    expect(replace).toHaveBeenCalledOnce();
    expect(replace).toHaveBeenCalledWith({ query: { page: "0", source: "profile" } });
    expect(fetchMyAnswers).toHaveBeenCalledTimes(2);
  });

  it("이전 페이지 응답은 더 최신 URL을 교정하지 않는다", async () => {
    let resolveOld!: (value: unknown) => void;
    route.query = { page: "8" };
    fetchMyAnswers.mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve; }));
    mount(AnswerHistoryView, { global: { stubs: { RouterLink: true } } });

    fetchMyAnswers.mockResolvedValueOnce({ content: [], page: 1, size: 20, totalElements: 21, totalPages: 2 });
    route.query = { page: "1", source: "newer" };
    await flushPromises();
    resolveOld({ content: [], page: 8, size: 20, totalElements: 1, totalPages: 1 });
    await flushPromises();

    expect(replace).not.toHaveBeenCalled();
    expect(route.query).toEqual({ page: "1", source: "newer" });
  });
});
