import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError, clearCsrfToken, get, post, setSessionExpiredHandler } from "@/api/client";

describe("HTTP 인증 만료 경계", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    setSessionExpiredHandler(undefined);
    clearCsrfToken();
  });

  it("보호 요청의 401은 세션 만료를 알린 뒤 원래 오류를 전달한다", async () => {
    const expired = vi.fn();
    setSessionExpiredHandler(() => expired);
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: "인증 필요" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    })));

    await expect(get("/api/questions", { authentication: "required" })).rejects.toEqual(
      expect.objectContaining({ status: 401 }),
    );
    expect(expired).toHaveBeenCalledOnce();
  });

  it("로그인 자격 증명 401은 세션 만료로 알리지 않는다", async () => {
    const expired = vi.fn();
    setSessionExpiredHandler(() => expired);
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: "로그인 실패" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    })));

    await expect(post("/api/auth/login", {}, undefined, { authentication: "credentials" }))
      .rejects.toBeInstanceOf(ApiClientError);
    expect(expired).not.toHaveBeenCalled();
  });

  it("보호 요청의 500은 세션 만료로 알리지 않고 다음 요청을 허용한다", async () => {
    const expired = vi.fn();
    setSessionExpiredHandler(() => expired);
    vi.stubGlobal("fetch", vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: "서버 오류" }), { status: 500 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ value: "ok" }), { status: 200 })));

    await expect(get("/api/questions", { authentication: "required" })).rejects.toMatchObject({ status: 500 });
    await expect(get<{ value: string }>("/api/questions", { authentication: "required" }))
      .resolves.toEqual({ value: "ok" });
    expect(expired).not.toHaveBeenCalled();
  });

  it("세션 만료 후속 처리 실패가 원래 401 오류를 덮지 않는다", async () => {
    const navigationFailure = new Error("navigation failed");
    setSessionExpiredHandler(() => async () => { throw navigationFailure; });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: "인증 필요" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    })));

    await expect(get("/api/questions", { authentication: "required" })).rejects.toMatchObject({
      name: "ApiClientError",
      status: 401,
      message: "인증 필요",
    });
  });

  it("서버 오류의 코드와 requestId를 진단 정보로 보존한다", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: "INTERNAL_SERVER_ERROR",
      message: "서버 오류",
      fieldErrors: [],
      requestId: "1e85b909-2114-47be-a1c3-1fa47a4a7235",
    }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    })));

    await expect(get("/api/questions")).rejects.toMatchObject({
      status: 500,
      code: "INTERNAL_SERVER_ERROR",
      requestId: "1e85b909-2114-47be-a1c3-1fa47a4a7235",
    });
  });

  it("쓰기 요청은 CSRF 토큰을 재사용하고 초기화 후 다시 조회한다", async () => {
    const fetchRequest = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "first", headerName: "X-CSRF-TOKEN" })))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "second", headerName: "X-CSRF-TOKEN" })))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchRequest);

    await post("/api/first");
    await post("/api/second");
    clearCsrfToken();
    await post("/api/third");

    expect(fetchRequest).toHaveBeenCalledTimes(5);
    expect(fetchRequest.mock.calls[1]?.[1].headers["X-CSRF-TOKEN"]).toBe("first");
    expect(fetchRequest.mock.calls[2]?.[1].headers["X-CSRF-TOKEN"]).toBe("first");
    expect(fetchRequest.mock.calls[4]?.[1].headers["X-CSRF-TOKEN"]).toBe("second");
  });
});
