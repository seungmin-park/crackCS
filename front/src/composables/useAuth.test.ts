import { beforeEach, describe, expect, it, vi } from "vitest";

const { fetchCurrentMember, clearCsrfToken, clearPendingAnswerSubmissions } = vi.hoisted(() => ({
  fetchCurrentMember: vi.fn(),
  clearCsrfToken: vi.fn(),
  clearPendingAnswerSubmissions: vi.fn(),
}));
vi.mock("@/composables/useAnswerSubmission", () => ({ clearPendingAnswerSubmissions }));

vi.mock("@/api/auth", () => ({
  fetchCurrentMember,
  login: vi.fn(),
  logout: vi.fn(),
}));
vi.mock("@/api/client", async (importOriginal) => {
  const original = await importOriginal<typeof import("@/api/client")>();
  return { ...original, clearCsrfToken };
});

describe("인증 상태 composable", () => {
  beforeEach(() => {
    vi.resetModules();
    fetchCurrentMember.mockReset();
    clearCsrfToken.mockReset();
    clearPendingAnswerSubmissions.mockReset();
  });

  it("서버 오류 뒤에는 인증을 확정하지 않고 다음 복구에서 재조회한다", async () => {
    fetchCurrentMember.mockRejectedValueOnce(new Error("일시 장애"))
      .mockResolvedValueOnce({ id: 1, nickname: "크랙러", role: "USER", status: "ACTIVE" });
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();

    await expect(auth.restoreAuthentication()).rejects.toThrow("일시 장애");
    expect(auth.authenticationResolved.value).toBe(false);
    await auth.restoreAuthentication();

    expect(auth.currentMember.value?.nickname).toBe("크랙러");
    expect(auth.authenticationResolved.value).toBe(true);
    expect(fetchCurrentMember).toHaveBeenCalledTimes(2);
  });

  it("401은 익명 상태로 확정하고 반복 조회하지 않는다", async () => {
    const { ApiClientError } = await import("@/api/client");
    fetchCurrentMember.mockRejectedValue(new ApiClientError(401, "인증 필요"));
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();

    await auth.restoreAuthentication();
    await auth.restoreAuthentication();

    expect(auth.currentMember.value).toBeNull();
    expect(auth.authenticationResolved.value).toBe(true);
    expect(fetchCurrentMember).toHaveBeenCalledOnce();
  });

  it("애플리케이션을 새로 열면 서버 세션에서 현재 회원을 복구한다", async () => {
    fetchCurrentMember.mockResolvedValue({ id: 1, nickname: "크랙러", role: "USER", status: "ACTIVE" });
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();

    await auth.restoreAuthentication();

    expect(fetchCurrentMember).toHaveBeenCalledOnce();
    expect(auth.currentMember.value?.nickname).toBe("크랙러");
    expect(auth.authenticationResolved.value).toBe(true);
  });

  it("로그아웃 상태 정리 시 탭에 남은 미확정 답변도 제거한다", async () => {
    const { useAuth } = await import("@/composables/useAuth");
    useAuth().clearAuthenticationState();
    expect(clearPendingAnswerSubmissions).toHaveBeenCalledOnce();
  });
});
