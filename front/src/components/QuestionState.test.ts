import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";

import QuestionState from "./QuestionState.vue";

describe("질문 상태 안내", () => {
  it("기본 상태는 빈 결과 안내로 노출하고 kind 속성을 DOM에 남기지 않는다", () => {
    const wrapper = mount(QuestionState, {
      props: { title: "결과가 없어요", description: "조건을 바꿔 보세요." },
    });

    const panel = wrapper.get("section");
    expect(panel.attributes("role")).toBe("status");
    expect(panel.attributes("aria-busy")).toBeUndefined();
    expect(panel.attributes("kind")).toBeUndefined();
    expect(panel.attributes("data-kind")).toBe("empty");
  });

  it("불러오는 상태는 보조 기술에 진행 중임을 알린다", () => {
    const wrapper = mount(QuestionState, {
      props: {
        kind: "loading",
        title: "권한을 확인하고 있어요",
        description: "잠시 기다려 주세요.",
      },
    });

    const panel = wrapper.get("section");
    expect(panel.attributes("role")).toBe("status");
    expect(panel.attributes("aria-busy")).toBe("true");
    expect(panel.attributes("kind")).toBeUndefined();
  });

  it("오류 상태는 즉시 알림으로 노출한다", () => {
    const wrapper = mount(QuestionState, {
      props: {
        kind: "error",
        title: "불러오지 못했어요",
        description: "다시 시도해 주세요.",
      },
    });

    const panel = wrapper.get("section");
    expect(panel.attributes("role")).toBe("alert");
    expect(panel.attributes("aria-busy")).toBeUndefined();
    expect(panel.attributes("kind")).toBeUndefined();
  });
});
