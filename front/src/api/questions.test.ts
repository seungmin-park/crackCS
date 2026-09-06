import { describe, expect, it, vi } from "vitest";
import { fetchQuestions } from "./questions";
const { get } = vi.hoisted(() => ({ get: vi.fn() }));
vi.mock("./client", () => ({ get }));

describe("문제 목록 요청", () => {
  it("선택한 페이지와 난이도를 서버에 전달한다", async () => {
    await fetchQuestions({ page: 2, difficulty: "ADVANCED" });
    const url = new URL(get.mock.calls.at(-1)![0], "https://example.test");
    expect(url.searchParams.get("page")).toBe("2");
    expect(url.searchParams.get("difficulty")).toBe("ADVANCED");
    expect(url.searchParams.get("size")).toBe("20");
  });
});
