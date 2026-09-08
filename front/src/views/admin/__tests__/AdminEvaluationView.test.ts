import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { fetchAdminEvaluations, fetchAdminEvaluation } = vi.hoisted(() => ({
  fetchAdminEvaluations: vi.fn(), fetchAdminEvaluation: vi.fn(),
}));
vi.mock("@/api/admin", () => ({ fetchAdminEvaluations, fetchAdminEvaluation }));

import AdminEvaluationView from "@/views/admin/AdminEvaluationView.vue";

describe("관리자 평가 검토 화면", () => {
  beforeEach(() => {
    fetchAdminEvaluations.mockReset();
    fetchAdminEvaluation.mockReset();
    fetchAdminEvaluations.mockResolvedValue({ content: [{ evaluationId: 9, answerId: 8, questionId: 7,
      status: "FAILED", failureCode: "PROVIDER_TIMEOUT", modelName: "gpt-5.6-terra",
      evaluatorVersion: "os-evaluator-v1", occurredAt: "2026-09-08T00:00:00" }],
      page: 0, size: 20, totalElements: 1, totalPages: 1 });
    fetchAdminEvaluation.mockResolvedValue({ evaluationId: 9, answerId: 8, questionId: 7,
      questionContent: "프로세스란?", answerContent: "프로그램 실행 인스턴스",
      status: "FAILED", failureCode: "PROVIDER_TIMEOUT", modelName: "gpt-5.6-terra",
      evaluatorVersion: "os-evaluator-v1", occurredAt: "2026-09-08T00:00:00", evidence: [] });
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
});
