import { describe, expect, it } from "vitest";

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
});
