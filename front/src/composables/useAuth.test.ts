import { beforeEach, describe, expect, it, vi } from "vitest";

const { fetchCurrentMember, clearCsrfToken } = vi.hoisted(() => ({
  fetchCurrentMember: vi.fn(),
  clearCsrfToken: vi.fn(),
}));

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
    fetchCurrentMember.mockReset();
    clearCsrfToken.mockReset();
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
});
