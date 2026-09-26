import { describe, expect, it, vi } from "vitest";

const { clearCsrfToken, clearPendingAnswerSubmissions } = vi.hoisted(() => ({
  clearCsrfToken: vi.fn(),
  clearPendingAnswerSubmissions: vi.fn(),
}));

vi.mock("@/api/client", () => ({ clearCsrfToken }));
vi.mock("@/composables/useAnswerSubmission", () => ({ clearPendingAnswerSubmissions }));

import { clearSessionData } from "@/composables/sessionData";

describe("세션 데이터 정리", () => {
  it("인증 종료 시 답변 임시 데이터와 CSRF 토큰을 함께 지운다", () => {
    clearSessionData();

    expect(clearPendingAnswerSubmissions).toHaveBeenCalledOnce();
    expect(clearCsrfToken).toHaveBeenCalledOnce();
  });
});
