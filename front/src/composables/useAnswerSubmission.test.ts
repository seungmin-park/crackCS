import { beforeEach, describe, expect, it, vi } from "vitest";

import { useAnswerSubmission } from "./useAnswerSubmission";
import { ApiClientError } from "@/api/client";

describe("답변 제출 복구", () => {
  beforeEach(() => {
    const values = new Map<string, string>();
    vi.stubGlobal("sessionStorage", {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value),
      removeItem: (key: string) => values.delete(key),
      clear: () => values.clear(),
    });
  });

  it("통신 결과가 불확실하면 같은 payload를 보존하고 새 입력과 분리한다", async () => {
    const send = vi.fn().mockRejectedValue(new TypeError("network"));
    const submission = useAnswerSubmission(42, 7, send, () => "request-1");
    submission.content.value = "처음 제출한 답변";

    await submission.submit();
    submission.content.value = "나중에 고친 답변";
    expect(submission.canSubmit.value).toBe(false);
    await submission.retry();

    expect(send).toHaveBeenNthCalledWith(1, 7, { requestId: "request-1", content: "처음 제출한 답변" });
    expect(send).toHaveBeenNthCalledWith(2, 7, { requestId: "request-1", content: "처음 제출한 답변" });
  });

  it("새 인스턴스가 저장된 미확정 제출을 복구한다", () => {
    sessionStorage.setItem("crackcs:answer-submission:42:7", JSON.stringify({ requestId: "request-1", content: "복구할 답변" }));
    const submission = useAnswerSubmission(42, 7, vi.fn());
    expect(submission.pendingPayload.value).toEqual({ requestId: "request-1", content: "복구할 답변" });
  });

  it("제출 중 중복 클릭은 요청을 한 번만 보낸다", async () => {
    let resolve!: (value: { id: number }) => void;
    const send = vi.fn(() => new Promise<{ id: number }>((done) => { resolve = done; }));
    const submission = useAnswerSubmission(42, 7, send, () => "request-1");
    submission.content.value = "답변";
    const first = submission.submit();
    const second = submission.submit();
    expect(send).toHaveBeenCalledTimes(1);
    resolve({ id: 1 });
    await Promise.all([first, second]);
  });

  it("다른 회원에게 이전 회원의 미확정 답변을 노출하지 않는다", () => {
    sessionStorage.setItem("crackcs:answer-submission:42:7", JSON.stringify({ requestId: "request-1", content: "회원 42의 답변" }));
    expect(useAnswerSubmission(99, 7, vi.fn()).pendingPayload.value).toBeNull();
  });

  it.each([408, 500, 503])("HTTP %s 응답은 처리 결과가 불확실하므로 같은 요청을 보존한다", async (status) => {
    const submission = useAnswerSubmission(42, 7, vi.fn().mockRejectedValue(new ApiClientError(status, "불확실")), () => "request-1");
    submission.content.value = "저장됐을 수 있는 답변";
    await submission.submit();
    expect(submission.pendingPayload.value).toEqual({ requestId: "request-1", content: "저장됐을 수 있는 답변" });
  });

  it("확정적인 4xx 거부는 미확정 요청을 제거한다", async () => {
    const submission = useAnswerSubmission(42, 7, vi.fn().mockRejectedValue(new ApiClientError(400, "공백 답변")), () => "request-1");
    submission.content.value = "거부된 답변";
    await submission.submit();
    expect(submission.pendingPayload.value).toBeNull();
  });
});
