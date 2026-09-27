import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import AdminFeedback from "./AdminFeedback.vue";

describe("관리자 입력 오류 안내", () => {
  it("입력별 이유를 공통 오류 아래에 빠짐없이 표시한다", () => {
    const wrapper = mount(AdminFeedback, { props: { error: "입력을 확인해 주세요.", fieldErrors: { topicId: "주제를 선택해 주세요.", "concepts[0].weight": "가중치는 양수여야 합니다." } } });
    expect(wrapper.get('[role="alert"]').text()).toContain("주제를 선택해 주세요.");
    expect(wrapper.get('[role="alert"]').text()).toContain("가중치는 양수여야 합니다.");
  });
});
