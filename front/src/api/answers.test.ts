import { beforeEach, describe, expect, it, vi } from "vitest";

import { fetchAnswer, fetchAnswerEvaluation, fetchMyAnswers, submitAnswer } from "./answers";

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }));
vi.mock("./client", () => ({ get, post }));

describe("답변 API", () => {
  beforeEach(() => {
    get.mockReset();
    post.mockReset();
  });

  it("멱등 요청 ID와 답변 원문을 제출한다", async () => {
    await submitAnswer(7, { requestId: "request-1", content: "프로세스는 자원을 소유합니다." });

    expect(post).toHaveBeenCalledWith("/api/questions/7/answers", {
      content: "프로세스는 자원을 소유합니다.",
    }, { "Idempotency-Key": "request-1" });
  });

  it("내 답변 이력의 페이지 조건을 전달한다", async () => {
    await fetchMyAnswers({ page: 2, size: 10 });
    expect(get).toHaveBeenCalledWith("/api/members/me/answers?page=2&size=10");
  });

  it("답변과 평가를 각각 조회한다", async () => {
    await fetchAnswer(12);
    await fetchAnswerEvaluation(12);
    expect(get).toHaveBeenNthCalledWith(1, "/api/answers/12");
    expect(get).toHaveBeenNthCalledWith(2, "/api/answers/12/evaluation");
  });
});
