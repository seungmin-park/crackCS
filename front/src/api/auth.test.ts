import { beforeEach, describe, expect, it, vi } from "vitest";

import { login, logout } from "@/api/auth";
import { clearCsrfToken } from "@/api/client";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

describe("로그인 CSRF 토큰 경계", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    clearCsrfToken();
  });

  it("로그인에 성공하면 다음 쓰기 요청 전에 CSRF 토큰을 다시 발급받는다", async () => {
    let issued = 0;
    const fetchMock = vi.fn(async (path: string, _init?: RequestInit) => {
      if (path === "/api/auth/csrf") {
        issued += 1;
        return jsonResponse({ headerName: "X-CSRF-TOKEN", token: `token-${issued}` });
      }
      if (path === "/api/auth/login") return jsonResponse({ id: 1, nickname: "회원", role: "USER", status: "ACTIVE" });
      return new Response(null, { status: 204 });
    });
    vi.stubGlobal("fetch", fetchMock);

    await login({ email: "user@example.com", password: "correct horse battery staple" });
    await logout();

    const logoutCall = fetchMock.mock.calls.find(([path]) => path === "/api/auth/logout");
    expect(issued).toBe(2);
    expect(logoutCall?.[1]?.headers).toMatchObject({ "X-CSRF-TOKEN": "token-2" });
  });
});
