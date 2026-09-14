import { beforeEach, describe, expect, it, vi } from "vitest";

const { fetchCurrentMember, requestLogin, requestLogout, clearCsrfToken, clearPendingAnswerSubmissions } = vi.hoisted(() => ({
  fetchCurrentMember: vi.fn(),
  requestLogin: vi.fn(),
  requestLogout: vi.fn(),
  clearCsrfToken: vi.fn(),
  clearPendingAnswerSubmissions: vi.fn(),
}));
vi.mock("@/composables/useAnswerSubmission", () => ({ clearPendingAnswerSubmissions }));

vi.mock("@/api/auth", () => ({
  fetchCurrentMember,
  login: requestLogin,
  logout: requestLogout,
}));
vi.mock("@/api/client", async (importOriginal) => {
  const original = await importOriginal<typeof import("@/api/client")>();
  return { ...original, clearCsrfToken };
});

describe("인증 상태 composable", () => {
  beforeEach(() => {
    vi.resetModules();
    fetchCurrentMember.mockReset();
    requestLogin.mockReset();
    requestLogout.mockReset();
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

  it("동시에 요청한 인증 복구는 같은 서버 조회를 공유한다", async () => {
    let resolveMember!: (member: { id: number; nickname: string; role: "USER"; status: "ACTIVE" }) => void;
    fetchCurrentMember.mockReturnValue(new Promise((resolve) => { resolveMember = resolve; }));
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();

    const first = auth.restoreAuthentication();
    const second = auth.restoreAuthentication();
    resolveMember({ id: 1, nickname: "복구 회원", role: "USER", status: "ACTIVE" });
    await Promise.all([first, second]);

    expect(fetchCurrentMember).toHaveBeenCalledOnce();
    expect(auth.currentMember.value?.nickname).toBe("복구 회원");
  });

  it("늦게 끝난 인증 복구가 새 로그인 상태를 덮지 않는다", async () => {
    let resolveRestore!: (member: { id: number; nickname: string; role: "USER"; status: "ACTIVE" }) => void;
    fetchCurrentMember.mockReturnValue(new Promise((resolve) => { resolveRestore = resolve; }));
    requestLogin.mockResolvedValue({ id: 2, nickname: "새 로그인", role: "USER", status: "ACTIVE" });
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();

    const restore = auth.restoreAuthentication();
    await auth.login({ email: "new@example.com", password: "new-password" });
    resolveRestore({ id: 1, nickname: "이전 세션", role: "USER", status: "ACTIVE" });
    await restore;

    expect(auth.currentMember.value?.nickname).toBe("새 로그인");
  });

  it("로그아웃 뒤 늦게 성공한 로그인이 다시 인증하지 않는다", async () => {
    let resolveLogin!: (member: { id: number; nickname: string; role: "USER"; status: "ACTIVE" }) => void;
    requestLogin.mockReturnValue(new Promise((resolve) => { resolveLogin = resolve; }));
    requestLogout.mockResolvedValue(undefined);
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();

    const login = auth.login({ email: "old@example.com", password: "old-password" });
    await auth.logout();
    resolveLogin({ id: 1, nickname: "늦은 로그인", role: "USER", status: "ACTIVE" });
    await login;

    expect(auth.currentMember.value).toBeNull();
  });

  it("새 로그인 뒤 늦게 끝난 로그아웃이 새 세션을 지우지 않는다", async () => {
    requestLogin.mockResolvedValueOnce({ id: 1, nickname: "기존", role: "USER", status: "ACTIVE" });
    let resolveLogout!: () => void;
    requestLogout.mockReturnValue(new Promise<void>((resolve) => { resolveLogout = resolve; }));
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();
    await auth.login({ email: "old@example.com", password: "old-password" });

    const logout = auth.logout();
    requestLogin.mockResolvedValueOnce({ id: 2, nickname: "새 로그인", role: "USER", status: "ACTIVE" });
    await auth.login({ email: "new@example.com", password: "new-password" });
    resolveLogout();
    await logout;

    expect(auth.currentMember.value?.nickname).toBe("새 로그인");
  });

  it("로그아웃 네트워크 실패는 로그인 상태를 유지하고 오류를 전달한다", async () => {
    requestLogin.mockResolvedValue({ id: 1, nickname: "회원", role: "USER", status: "ACTIVE" });
    requestLogout.mockRejectedValue(new Error("network down"));
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();
    await auth.login({ email: "user@example.com", password: "password" });

    await expect(auth.logout()).rejects.toThrow("network down");

    expect(auth.currentMember.value?.nickname).toBe("회원");
  });

  it("로그아웃의 확인된 401은 로컬 인증 상태를 정리한다", async () => {
    const { ApiClientError } = await import("@/api/client");
    requestLogin.mockResolvedValue({ id: 1, nickname: "회원", role: "USER", status: "ACTIVE" });
    requestLogout.mockRejectedValue(new ApiClientError(401, "이미 만료됨"));
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();
    await auth.login({ email: "user@example.com", password: "password" });

    await auth.logout();

    expect(auth.currentMember.value).toBeNull();
  });

  it("이전 보호 요청의 만료 신호가 새 로그인을 지우지 않는다", async () => {
    requestLogin.mockResolvedValue({ id: 2, nickname: "새 로그인", role: "USER", status: "ACTIVE" });
    const { useAuth } = await import("@/composables/useAuth");
    const auth = useAuth();
    const expireIfCurrent = auth.captureSessionExpiration();

    await auth.login({ email: "new@example.com", password: "new-password" });
    const expired = expireIfCurrent();

    expect(expired).toBe(false);
    expect(auth.currentMember.value?.nickname).toBe("새 로그인");
  });
});
