import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import AnswerDetailView from "@/views/AnswerDetailView.vue";
import { ApiClientError } from "@/api/client";

const { fetchAnswer, fetchAnswerEvaluation, routeState } = vi.hoisted(() => ({ fetchAnswer: vi.fn(), fetchAnswerEvaluation: vi.fn(), routeState: { route: null as any } }));
vi.mock("@/api/answers", () => ({ fetchAnswer, fetchAnswerEvaluation }));
vi.mock("vue-router", async () => {
  const { reactive } = await import("vue");
  routeState.route = reactive({ params: { answerId: "31" } });
  return { useRoute: () => routeState.route };
});

const evaluating = { status: "EVALUATING", verdict: null, score: null, feedback: null, failureReason: null, concepts: [] };

describe("답변 상세 화면", () => {
  beforeEach(() => { vi.useFakeTimers(); fetchAnswer.mockReset(); fetchAnswerEvaluation.mockReset(); routeState.route.params.answerId = "31"; });
  afterEach(() => vi.useRealTimers());

  it("평가 중이면 다시 조회하고 완료되면 polling을 멈춘다", async () => {
    fetchAnswer.mockResolvedValue({ answerId: 31, questionId: 7, questionContent: "질문", content: "내 답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: evaluating });
    fetchAnswerEvaluation.mockResolvedValue({ ...evaluating, status: "EVALUATED", verdict: "CORRECT", score: 100, feedback: "핵심을 설명했습니다." });
    const wrapper = mount(AnswerDetailView, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } } });
    await flushPromises();
    expect(wrapper.text()).toContain("평가 중");
    await vi.advanceTimersByTimeAsync(2000);
    await flushPromises();
    expect(wrapper.text()).toContain("정답");
    expect(wrapper.text()).toContain("핵심을 설명했습니다.");
    await vi.advanceTimersByTimeAsync(4000);
    expect(fetchAnswerEvaluation).toHaveBeenCalledTimes(1);
  });

  it("화면을 떠나면 예약된 평가 조회를 취소한다", async () => {
    fetchAnswer.mockResolvedValue({ answerId: 31, questionId: 7, questionContent: "질문", content: "내 답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: evaluating });
    const wrapper = mount(AnswerDetailView, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } } });
    await flushPromises();
    wrapper.unmount();
    await vi.advanceTimersByTimeAsync(2000);
    expect(fetchAnswerEvaluation).not.toHaveBeenCalled();
  });

  it("화면을 떠난 뒤 진행 중이던 평가 요청이 실패해도 polling을 다시 예약하지 않는다", async () => {
    let reject!: (reason: unknown) => void;
    fetchAnswer.mockResolvedValue({ answerId: 31, questionId: 7, questionContent: "질문", content: "내 답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: evaluating });
    fetchAnswerEvaluation.mockReturnValue(new Promise((_resolve, fail) => { reject = fail; }));
    const wrapper = mount(AnswerDetailView, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } } });
    await flushPromises();
    await vi.advanceTimersByTimeAsync(2000);
    wrapper.unmount();
    reject(new TypeError("network"));
    await flushPromises();
    await vi.advanceTimersByTimeAsync(4000);
    expect(fetchAnswerEvaluation).toHaveBeenCalledTimes(1);
  });

  it("재사용된 라우트에서는 이전 답변 응답을 버리고 새 답변만 표시한다", async () => {
    let resolveOld!: (value: any) => void;
    fetchAnswer.mockImplementation((answerId: string) => answerId === "31" ? new Promise((done) => { resolveOld = done; }) : Promise.resolve({ answerId: 32, questionId: 8, questionContent: "새 질문", content: "새 답변", submittedAt: "2026-09-07T11:00:00Z", evaluation: { ...evaluating, status: "EVALUATED", verdict: "CORRECT", score: 100 } }));
    const wrapper = mount(AnswerDetailView, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } } });
    routeState.route.params.answerId = "32";
    await flushPromises();
    expect(wrapper.text()).toContain("새 질문");
    resolveOld({ answerId: 31, questionId: 7, questionContent: "오래된 질문", content: "오래된 답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: evaluating });
    await flushPromises();
    expect(wrapper.text()).toContain("새 질문");
    expect(wrapper.text()).not.toContain("오래된 질문");
  });

  it("검토 필요와 시스템 평가 실패를 서로 다른 상태로 설명한다", async () => {
    fetchAnswer.mockResolvedValue({ answerId: 31, questionId: 7, questionContent: "질문", content: "내 답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: { ...evaluating, status: "FAILED", failureReason: "provider timeout" } });
    const failed = mount(AnswerDetailView, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } } });
    await flushPromises();
    expect(failed.text()).toContain("평가 실패");
    expect(failed.text()).toContain("답변이 틀렸다는 뜻은 아닙니다");
    expect(failed.text()).not.toContain("다시 확인");
  });

  it("평가 조회가 404이면 polling을 끝내고 오류 화면을 표시한다", async () => {
    fetchAnswer.mockResolvedValue({ answerId: 31, questionId: 7, questionContent: "질문", content: "내 답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: evaluating });
    fetchAnswerEvaluation.mockRejectedValue(new ApiClientError(404, "답변을 찾을 수 없습니다."));
    const wrapper = mount(AnswerDetailView, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } } });
    await flushPromises();

    await vi.advanceTimersByTimeAsync(2000);
    await flushPromises();
    await vi.advanceTimersByTimeAsync(4000);

    expect(fetchAnswerEvaluation).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain("답변을 불러오지 못했어요");
  });

  it("일시적인 평가 조회 오류도 세 번 연속 발생하면 polling을 중단한다", async () => {
    fetchAnswer.mockResolvedValue({ answerId: 31, questionId: 7, questionContent: "질문", content: "내 답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: evaluating });
    fetchAnswerEvaluation.mockRejectedValue(new TypeError("network"));
    const wrapper = mount(AnswerDetailView, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } } });
    await flushPromises();

    await vi.advanceTimersByTimeAsync(8000);
    await flushPromises();

    expect(fetchAnswerEvaluation).toHaveBeenCalledTimes(3);
    expect(wrapper.text()).toContain("답변을 불러오지 못했어요");
  });

  it("평가 Concept 이름을 식별자 대신 표시한다", async () => {
    fetchAnswer.mockResolvedValue({ answerId: 31, questionId: 7, questionContent: "질문", content: "내 답변", submittedAt: "2026-09-07T10:00:00Z", evaluation: { ...evaluating, status: "EVALUATED", verdict: "CORRECT", score: 100, concepts: [{ conceptId: 11, conceptName: "스레드", verdict: "CORRECT", score: 100, feedback: "정확함" }] } });
    const wrapper = mount(AnswerDetailView, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } } });
    await flushPromises();

    expect(wrapper.text()).toContain("스레드 · 정답");
    expect(wrapper.text()).not.toContain("개념 11");
  });
});
