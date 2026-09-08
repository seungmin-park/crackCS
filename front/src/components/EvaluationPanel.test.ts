import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";

import EvaluationPanel from "./EvaluationPanel.vue";

describe("평가 결과 패널", () => {
  it("강점과 누락 및 평가 근거의 문서 버전과 범위를 표시한다", () => {
    const wrapper = mount(EvaluationPanel, { props: { evaluation: {
      status: "EVALUATED", verdict: "PARTIALLY_CORRECT", score: 50, feedback: "핵심은 맞습니다.",
      failureReason: null, concepts: [], strengths: ["프로세스의 자원 소유를 설명함"],
      omissions: ["스레드의 자원 공유가 빠짐"], misconceptions: [],
      evidence: [{ chunkId: 21, documentTitle: "프로세스와 스레드", documentVersion: 2,
        startOffset: 10, endOffset: 42, content: "스레드는 프로세스 자원을 공유한다." }],
    } } });

    expect(wrapper.text()).toContain("프로세스의 자원 소유를 설명함");
    expect(wrapper.text()).toContain("스레드의 자원 공유가 빠짐");
    expect(wrapper.text()).toContain("프로세스와 스레드 · v2 · 10–42");
    expect(wrapper.text()).toContain("스레드는 프로세스 자원을 공유한다.");
  });

  it("검토 필요 상태를 완료 평가와 구분하고 지식 상태에 반영되지 않음을 설명한다", () => {
    const wrapper = mount(EvaluationPanel, { props: { evaluation: {
      status: "NEEDS_REVIEW", verdict: "NEEDS_REVIEW", score: null, feedback: null,
      failureReason: "EVIDENCE_NOT_FOUND", concepts: [], strengths: [], omissions: [],
      misconceptions: [], evidence: [],
    } } });

    expect(wrapper.text()).toContain("사람의 검토가 필요합니다");
    expect(wrapper.text()).toContain("학습 상태에는 반영되지 않습니다");
  });
});
