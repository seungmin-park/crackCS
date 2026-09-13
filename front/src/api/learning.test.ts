import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchKnowledgeStates, fetchProgress } from "@/api/learning";

afterEach(() => vi.unstubAllGlobals());

describe("내 학습 정보 요청", () => {
  it("지식 지도는 현재 세션의 회원 경로로 조회한다", async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify({ topics: [] }), { status: 200 }));
    vi.stubGlobal("fetch", fetch);
    expect(await fetchKnowledgeStates()).toEqual({ topics: [] });
    expect(fetch).toHaveBeenCalledWith("/api/members/me/knowledge-states", expect.objectContaining({ method: "GET", credentials: "same-origin" }));
  });

  it("학습 홈 인증 실패를 정상 응답으로 취급하지 않는다", async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: "로그인이 필요합니다." }), { status: 401 }));
    vi.stubGlobal("fetch", fetch);
    await expect(fetchProgress()).rejects.toMatchObject({ status: 401 });
    expect(fetch).toHaveBeenCalledWith("/api/members/me/progress", expect.objectContaining({ method: "GET", credentials: "same-origin" }));
  });
});
