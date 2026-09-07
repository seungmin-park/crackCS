import { reactive } from "vue";
import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import AnswerHistoryView from "@/views/AnswerHistoryView.vue";

const { fetchMyAnswers } = vi.hoisted(() => ({ fetchMyAnswers: vi.fn() }));
vi.mock("@/api/answers", () => ({ fetchMyAnswers }));
const route = reactive({ query: {} as Record<string, string> });
const push = vi.fn(async (location: { query: Record<string, string> }) => { route.query = location.query; });
vi.mock("vue-router", () => ({ useRoute: () => route, useRouter: () => ({ push }) }));

enableAutoUnmount(afterEach);

describe("답변 이력 화면", () => {
  beforeEach(() => { fetchMyAnswers.mockReset(); route.query = {}; push.mockClear(); });

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
  it("다음 페이지로 이동하면 이전 답변 이력을 불러온다", async () => {
    fetchMyAnswers.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 21, totalPages: 2 });
    const wrapper = mount(AnswerHistoryView, { global: { stubs: { RouterLink: true } } });
    await flushPromises();
    await wrapper.get("button[aria-label='다음 페이지']").trigger("click");
    await flushPromises();
    expect(fetchMyAnswers).toHaveBeenLastCalledWith({ page: 1, size: 20 });
    wrapper.unmount();
  });
});
