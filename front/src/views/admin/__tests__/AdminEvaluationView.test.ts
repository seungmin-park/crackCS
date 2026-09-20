import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { reactive } from "vue";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
enableAutoUnmount(afterEach);
const route = reactive({ query: {} as Record<string, string> });
const push = vi.fn(async ({ query }: { query: Record<string, string> }) => { route.query = query; });
const replace = vi.fn(async ({ query }: { query: Record<string, string> }) => { route.query = query; });
vi.mock("vue-router", () => ({ useRoute: () => route, useRouter: () => ({ push, replace }) }));

const { fetchAdminEvaluations, fetchAdminEvaluation } = vi.hoisted(() => ({
  fetchAdminEvaluations: vi.fn(), fetchAdminEvaluation: vi.fn(),
}));
vi.mock("@/api/admin/evaluations", () => ({ fetchAdminEvaluations, fetchAdminEvaluation }));

import AdminEvaluationView from "@/views/admin/AdminEvaluationView.vue";

describe("관리자 평가 검토 화면", () => {
  beforeEach(() => {
    fetchAdminEvaluations.mockReset();
    fetchAdminEvaluation.mockReset();
    route.query = {}; push.mockClear(); replace.mockClear();
    fetchAdminEvaluations.mockResolvedValue({ content: [{ evaluationId: 9, answerId: 8, questionId: 7,
      status: "FAILED", failureCode: "PROVIDER_TIMEOUT", modelName: "gpt-5.6-terra",
      evaluatorVersion: "os-evaluator-v1", occurredAt: "2026-09-08T00:00:00" }],
      page: 0, size: 20, totalElements: 1, totalPages: 1 });
    fetchAdminEvaluation.mockResolvedValue({ evaluationId: 9, answerId: 8, questionId: 7,
      questionContent: "프로세스란?", answerContent: "프로그램 실행 인스턴스",
      status: "FAILED", failureCode: "PROVIDER_TIMEOUT", modelName: "gpt-5.6-terra",
      evaluatorVersion: "os-evaluator-v1", occurredAt: "2026-09-08T00:00:00", evidence: [] });
  });

  it("범위를 벗어난 평가 page를 마지막 유효 page로 replace한다", async () => {
    route.query = { page: "4", status: "FAILED" };
    fetchAdminEvaluations.mockResolvedValueOnce({ content: [], page: 4, size: 20, totalElements: 35, totalPages: 2 });
    mount(AdminEvaluationView);
    await flushPromises();
    expect(replace).toHaveBeenCalledWith({ query: { page: "1", status: "FAILED" } });
  });

  it("URL의 평가 page와 상태를 복원하고 필터 변경 시 page를 0으로 기록한다", async () => {
    route.query = { page: "2", status: "FAILED" };
    fetchAdminEvaluations.mockResolvedValueOnce({ content: [], page: 2, size: 20, totalElements: 70, totalPages: 4 });
    const wrapper = mount(AdminEvaluationView);
    await flushPromises();
    expect(fetchAdminEvaluations).toHaveBeenCalledWith({ status: "FAILED", page: 2, size: 20 });
    await wrapper.get("select").setValue("NEEDS_REVIEW");
    expect(push).toHaveBeenCalledWith({ query: { page: "0", status: "NEEDS_REVIEW" } });
  });

  it("브라우저 이동으로 평가 query가 바뀌면 새 조건을 조회한다", async () => {
    const wrapper = mount(AdminEvaluationView);
    await flushPromises(); fetchAdminEvaluations.mockClear();
    route.query = { page: "1", status: "NEEDS_REVIEW" };
    await flushPromises();
    expect(fetchAdminEvaluations).toHaveBeenCalledWith({ status: "NEEDS_REVIEW", page: 1, size: 20 });
    wrapper.unmount();
  });

  it("실패 목록에서 항목을 선택하면 답변 원문과 안전한 실패 코드를 표시한다", async () => {
    const wrapper = mount(AdminEvaluationView);
    await flushPromises();
    await wrapper.get("button[data-evaluation-id='9']").trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("PROVIDER_TIMEOUT");
    expect(wrapper.text()).toContain("프로그램 실행 인스턴스");
    expect(fetchAdminEvaluations).toHaveBeenCalledWith({ status: "", page: 0, size: 20 });
    expect(fetchAdminEvaluation).toHaveBeenCalledWith(9);
  });

  it("필터 변경 전 목록 응답과 완료 상태를 무시한다", async () => {
    let resolveOld!: (value: object) => void;
    fetchAdminEvaluations
      .mockReturnValueOnce(new Promise(resolve => { resolveOld = resolve; }))
      .mockResolvedValueOnce({ content: [{ evaluationId: 20, status: "NEEDS_REVIEW", failureCode: null }], page: 0, size: 20, totalElements: 1, totalPages: 1 });
    const wrapper = mount(AdminEvaluationView);
    await wrapper.get("select").setValue("NEEDS_REVIEW");
    await flushPromises();

    resolveOld({ content: [{ evaluationId: 10, status: "FAILED", failureCode: "OLD" }], page: 0, size: 20, totalElements: 1, totalPages: 1 });
    await flushPromises();

    expect(wrapper.find("button[data-evaluation-id='20']").exists()).toBe(true);
    expect(wrapper.find("button[data-evaluation-id='10']").exists()).toBe(false);
    expect(wrapper.text()).not.toContain("평가를 불러오는 중");
  });

  it("늦은 상세 실패가 새 상세 성공을 오류로 덮지 않는다", async () => {
    let rejectOld!: (reason: Error) => void;
    fetchAdminEvaluation
      .mockReturnValueOnce(new Promise((_, reject) => { rejectOld = reject; }))
      .mockResolvedValueOnce({ evaluationId: 10, status: "NEEDS_REVIEW", failureCode: null, questionContent: "새 질문", answerContent: "새 답변", evidence: [] });
    const wrapper = mount(AdminEvaluationView);
    await flushPromises();
    const first = wrapper.get("button[data-evaluation-id='9']").trigger("click");
    await first;
    await wrapper.get("button[data-evaluation-id='9']").trigger("click");
    await flushPromises();
    rejectOld(new Error("old"));
    await flushPromises();

    expect(wrapper.text()).toContain("새 답변");
    expect(wrapper.text()).not.toContain("평가 정보를 불러오지 못했습니다.");
  });

  it("초기 목록 실패 뒤 재시도하면 목록과 로딩 상태를 복구한다", async () => {
    fetchAdminEvaluations.mockRejectedValueOnce(new Error("temporary"));
    const wrapper = mount(AdminEvaluationView);
    await flushPromises();
    expect(wrapper.text()).toContain("평가 목록을 불러오지 못했습니다.");
    expect(wrapper.text()).not.toContain("평가를 불러오는 중");

    fetchAdminEvaluations.mockResolvedValueOnce({ content: [{ evaluationId: 9, status: "FAILED", failureCode: "PROVIDER_TIMEOUT" }], page: 0, size: 20, totalElements: 1, totalPages: 1 });
    await wrapper.get("button[data-retry='list']").trigger("click");
    await flushPromises();
    expect(wrapper.find("button[data-evaluation-id='9']").exists()).toBe(true);
  });
});
