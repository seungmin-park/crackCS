import { describe, expect, it } from "vitest";
import { ApiClientError } from "@/api/client";
import { presentRequestError } from "./requestErrorPresentation";

describe("공통 요청 오류 표현", () => {
  it.each([
    [400, "validation", false], [401, "unauthenticated", false],
    [403, "forbidden", false], [404, "not-found", false],
    [409, "conflict", false], [408, "timeout", true], [429, "rate-limited", true],
  ] as const)("HTTP %i의 의미와 재시도 가능 여부를 구분한다", (status, kind, retryable) => {
    expect(presentRequestError(new ApiClientError(status, "안내"))).toMatchObject({ kind, retryable });
  });

  it("서버 내부 메시지와 필드 정보는 일반 안내에 포함하지 않는다", () => {
    const error = new ApiClientError(503, "database credential", [{ field: "secret", reason: "private" }]);
    const presented = presentRequestError(error);
    expect(presented.message).not.toContain("credential");
    expect(presented.fieldErrors).toEqual({});
  });
});
