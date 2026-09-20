import { ref } from "vue";
import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { FollowUpQuestionResponse } from "@/api/answers";
import FollowUpQuestionPanel from "./FollowUpQuestionPanel.vue";

const { fetchFollowUpQuestion, submitAnswer, push } = vi.hoisted(() => ({
  fetchFollowUpQuestion: vi.fn(),
  submitAnswer: vi.fn(),
  push: vi.fn(),
}));
const member = ref({ id: 41, role: "USER" });

vi.mock("@/api/answers", async (importOriginal) => {
  const original = await importOriginal<typeof import("@/api/answers")>();
  return { ...original, fetchFollowUpQuestion, submitAnswer };
});
vi.mock("@/composables/useAuth", () => ({ useAuth: () => ({ currentMember: member }) }));
vi.mock("vue-router", () => ({ useRouter: () => ({ push }) }));

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
const submittedAnswer = {
  answerId: 99,
  evaluationId: 199,
  questionId: 17,
  questionContent: ready.question.content,
  content: "각 스레드는 같은 프로세스 자원을 공유합니다.",
  submittedAt: "2026-09-20T16:00:00",
  evaluation: {
    status: "EVALUATING",
    verdict: null,
    score: null,
    feedback: null,
    failureReason: null,
    concepts: [],
    strengths: [],
    omissions: [],
    misconceptions: [],
    evidence: [],
  },
};

function mountPanel() {
  return mount(FollowUpQuestionPanel, {
    props: { answerId: 31 },
    global: {
      stubs: {
        RouterLink: { props: ["to"], template: "<a :data-to='typeof to === `string` ? to : to.path'><slot /></a>" },
      },
    },
  });
}

describe("후속 질문 패널", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    member.value = { id: 41, role: "USER" };
    fetchFollowUpQuestion.mockReset();
    submitAnswer.mockReset();
    push.mockReset();
    sessionStorage.clear();
  });
  afterEach(() => vi.useRealTimers());

  it.each([
    [{ status: "PENDING", reason: null, question: null }, "후속 질문을 준비하고 있어요"],
    [{ status: "PROCESSING", reason: null, question: null }, "후속 질문을 만들고 있어요"],
    [{ status: "FAILED", reason: "PROVIDER_TIMEOUT", question: null }, "후속 질문을 만들지 못했어요"],
    [{ status: "UNAVAILABLE", reason: "EVALUATION_NOT_ELIGIBLE", question: null }, "이번 답변에는 후속 질문이 없어요"],
    [{ status: "UNAVAILABLE", reason: "CONTENT_UNAVAILABLE", question: null }, "이번 답변에는 후속 질문이 없어요"],
  ] as const)("상태에 맞는 안내를 표시한다: %s", async (response, message) => {
    fetchFollowUpQuestion.mockResolvedValue(response);

    const wrapper = mountPanel();
    await flushPromises();

    expect(wrapper.text()).toContain(message);
    expect(wrapper.find("form").exists()).toBe(false);
    wrapper.unmount();
  });

  it("후속 답변에서는 다음 기본 문제 학습 경로를 제공한다", async () => {
    fetchFollowUpQuestion.mockResolvedValue({ status: "UNAVAILABLE", reason: "FOLLOW_UP_LIMIT", question: null });

    const wrapper = mountPanel();
    await flushPromises();

    expect(wrapper.text()).toContain("후속 학습을 마쳤어요");
    expect(wrapper.get("a").attributes("data-to")).toBe("/questions");
    wrapper.unmount();
  });

  it("READY 질문을 공개 문제 링크 없이 인라인 폼으로 제출하고 새 답변 상세로 이동한다", async () => {
    fetchFollowUpQuestion.mockResolvedValue(ready);
    submitAnswer.mockResolvedValue(submittedAnswer);
    const wrapper = mountPanel();
    await flushPromises();

    expect(wrapper.text()).toContain("운영체제");
    expect(wrapper.text()).toContain("중급");
    expect(wrapper.text()).toContain("프로세스와 스레드를 실제 서버 사례로 비교해 보세요.");
    expect(wrapper.find("a[data-to='/questions/17']").exists()).toBe(false);

    await wrapper.get("textarea").setValue("각 스레드는 같은 프로세스 자원을 공유합니다.");
    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(submitAnswer).toHaveBeenCalledWith(17, {
      requestId: expect.any(String),
      content: "각 스레드는 같은 프로세스 자원을 공유합니다.",
    });
    expect(push).toHaveBeenCalledWith({ name: "answer-detail", params: { answerId: 99 } });
    wrapper.unmount();
  });

  it("후속 답변도 회원과 질문별 미확정 제출 키를 사용한다", async () => {
    let resolveSubmission!: (answer: typeof submittedAnswer) => void;
    fetchFollowUpQuestion.mockResolvedValue(ready);
    submitAnswer.mockReturnValue(new Promise((resolve) => { resolveSubmission = resolve; }));
    const wrapper = mountPanel();
    await flushPromises();

    await wrapper.get("textarea").setValue("저장 여부를 확인해야 하는 후속 답변");
    await wrapper.get("form").trigger("submit");

    const pending = JSON.parse(sessionStorage.getItem("crackcs:answer-submission:41:17") ?? "null");
    expect(pending).toEqual({ requestId: expect.any(String), content: "저장 여부를 확인해야 하는 후속 답변" });

    resolveSubmission(submittedAnswer);
    await flushPromises();
    expect(sessionStorage.getItem("crackcs:answer-submission:41:17")).toBeNull();
    wrapper.unmount();
  });

  it("조회가 연속 실패하면 버튼으로 같은 답변의 후속 질문을 다시 조회한다", async () => {
    fetchFollowUpQuestion.mockRejectedValue(new TypeError("network"));
    const wrapper = mountPanel();
    await flushPromises();
    await vi.runAllTimersAsync();

    expect(fetchFollowUpQuestion).toHaveBeenCalledTimes(3);
    expect(wrapper.text()).toContain("후속 질문을 확인하지 못했어요");

    fetchFollowUpQuestion.mockResolvedValueOnce(ready);
    await wrapper.get("button").trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("프로세스와 스레드를 실제 서버 사례로 비교해 보세요.");
    wrapper.unmount();
  });
});
