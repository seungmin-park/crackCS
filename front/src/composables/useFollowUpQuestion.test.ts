import { defineComponent, h, ref } from "vue";
import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { FollowUpQuestionResponse } from "@/api/answers";
import { ApiClientError } from "@/api/client";
import { useFollowUpQuestion } from "./useFollowUpQuestion";

const pending: FollowUpQuestionResponse = { status: "PENDING", reason: null, question: null };
const ready: FollowUpQuestionResponse = {
  status: "READY",
  reason: null,
  question: {
    id: 17,
    topic: { id: 1, code: "OS", name: "운영체제" },
    difficulty: "INTERMEDIATE",
    content: "프로세스와 스레드를 실제 서버 사례로 비교해 보세요.",
  },
};

function mountHarness(request: (answerId: number | string) => Promise<FollowUpQuestionResponse>) {
  const answerId = ref("31");
  let followUp!: ReturnType<typeof useFollowUpQuestion>;
  const wrapper = mount(defineComponent({
    setup() {
      followUp = useFollowUpQuestion(answerId, request);
      return () => h("div");
    },
  }));
  return { answerId, followUp, wrapper };
}

describe("후속 질문 조회", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("생성 중 상태를 polling하고 READY가 되면 멈춘다", async () => {
    const request = vi.fn()
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(ready);
    const { followUp } = mountHarness(request);
    await flushPromises();

    expect(followUp.result.value?.status).toBe("PENDING");
    await vi.advanceTimersByTimeAsync(2_000);
    await flushPromises();

    expect(followUp.result.value).toEqual(ready);
    await vi.advanceTimersByTimeAsync(10_000);
    expect(request).toHaveBeenCalledTimes(2);
  });

  it("생성 상태 polling을 15번으로 제한하고 수동 재시도로 복구한다", async () => {
    const request = vi.fn().mockResolvedValue(pending);
    const { followUp } = mountHarness(request);
    await flushPromises();

    await vi.runAllTimersAsync();
    expect(request).toHaveBeenCalledTimes(15);
    expect(followUp.error.value).toEqual({ kind: "timeout", retryable: true });

    request.mockResolvedValueOnce(ready);
    await followUp.retry();
    expect(followUp.result.value).toEqual(ready);
    expect(followUp.error.value).toBeUndefined();
  });

  it("일시 오류는 세 번까지만 자동 재시도하고 수동 재시도로 복구한다", async () => {
    const request = vi.fn().mockRejectedValue(new TypeError("network"));
    const { followUp } = mountHarness(request);
    await flushPromises();

    await vi.runAllTimersAsync();
    expect(request).toHaveBeenCalledTimes(3);
    expect(followUp.error.value).toEqual({ kind: "temporary", retryable: true });

    request.mockResolvedValueOnce(ready);
    await followUp.retry();
    expect(followUp.result.value).toEqual(ready);
  });

  it("생성 대기와 일시 오류가 섞여도 전체 조회 예산을 넘지 않는다", async () => {
    const request = vi.fn()
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockResolvedValueOnce(pending)
      .mockRejectedValue(new TypeError("network"));
    const { followUp } = mountHarness(request);
    await flushPromises();

    await vi.runAllTimersAsync();

    expect(request).toHaveBeenCalledTimes(15);
    expect(followUp.error.value).toEqual({ kind: "timeout", retryable: true });
  });

  it.each([403, 404])("HTTP %s 오류는 자동 재시도하지 않는다", async (status) => {
    const request = vi.fn().mockRejectedValue(new ApiClientError(status, "terminal"));
    const { followUp } = mountHarness(request);
    await flushPromises();

    await vi.advanceTimersByTimeAsync(10_000);

    expect(request).toHaveBeenCalledTimes(1);
    expect(followUp.error.value).toEqual({ kind: status === 403 ? "forbidden" : "not-found", retryable: false });
  });

  it("답변 ID가 바뀌면 이전 응답을 버린다", async () => {
    let resolveOld!: (value: FollowUpQuestionResponse) => void;
    const request = vi.fn((answerId: number | string) => answerId === "31"
      ? new Promise<FollowUpQuestionResponse>((resolve) => { resolveOld = resolve; })
      : Promise.resolve(ready));
    const { answerId, followUp } = mountHarness(request);

    answerId.value = "32";
    await flushPromises();
    expect(followUp.result.value).toEqual(ready);

    resolveOld({ status: "UNAVAILABLE", reason: "CONTENT_UNAVAILABLE", question: null });
    await flushPromises();
    expect(followUp.result.value).toEqual(ready);
  });

  it("화면을 떠나면 예약된 polling을 취소한다", async () => {
    const request = vi.fn().mockResolvedValue(pending);
    const { wrapper } = mountHarness(request);
    await flushPromises();

    wrapper.unmount();
    await vi.advanceTimersByTimeAsync(4_000);

    expect(request).toHaveBeenCalledTimes(1);
  });
});
