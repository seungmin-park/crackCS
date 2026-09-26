import { describe, expect, it } from "vitest";
import { clearCsrfToken, getCsrfToken } from "@/api/csrfTokenStore";

describe("CSRF 토큰 저장소", () => {
  it("요청에서 얻은 토큰을 보관하고 세션 정리 시 버린다", async () => {
    let fetchCount = 0;
    const fetchToken = async () => ({ token: String(++fetchCount), headerName: "X-CSRF-TOKEN" });

    expect(await getCsrfToken(fetchToken)).toEqual({ token: "1", headerName: "X-CSRF-TOKEN" });
    expect(await getCsrfToken(fetchToken)).toEqual({ token: "1", headerName: "X-CSRF-TOKEN" });
    clearCsrfToken();
    expect(await getCsrfToken(fetchToken)).toEqual({ token: "2", headerName: "X-CSRF-TOKEN" });
    clearCsrfToken();
  });
});
