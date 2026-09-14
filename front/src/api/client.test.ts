import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError, get, post, setSessionExpiredHandler } from "@/api/client";

describe("HTTP 인증 만료 경계", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    setSessionExpiredHandler(undefined);
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
});
