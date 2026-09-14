import { describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/api/client";
import { useAdminFeedback } from "@/composables/useAdminFeedback";

describe("관리자 폼 피드백", () => {
  it("등록 성공 메시지를 공통 상태로 기록한다", async () => {
    const feedback = useAdminFeedback();
    await feedback.execute(async () => ({ id: 1 }), "등록했습니다.");
    expect(feedback.successMessage.value).toBe("등록했습니다.");
    expect(feedback.formError.value).toBe("");
  });

  it("API validation 오류를 폼 오류와 필드별 오류로 변환한다", async () => {
    const feedback = useAdminFeedback();
    await feedback.execute(
      async () => { throw new ApiClientError(400, "요청 값이 올바르지 않습니다.", [{ field: "code", reason: "code는 필수입니다." }]); },
      "등록했습니다.",
    );
    expect(feedback.formError.value).toBe("요청 값이 올바르지 않습니다.");
    expect(feedback.fieldErrors.value.code).toBe("code는 필수입니다.");
  });

  it("명령이 진행 중이면 두 번째 명령을 실행하지 않는다", async () => {
    let resolve!: () => void;
    const firstAction = vi.fn(() => new Promise<void>(done => { resolve = done; }));
    const secondAction = vi.fn(async () => undefined);
    const feedback = useAdminFeedback();

    const first = feedback.execute(firstAction, "완료했습니다.");
    const second = await feedback.execute(secondAction, "중복 완료");

    expect(second).toBeUndefined();
    expect(secondAction).not.toHaveBeenCalled();
    expect(feedback.submitting.value).toBe(true);
    resolve();
    await first;
  });
});
