import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/api/client";
import QuestionDetailView from "@/views/QuestionDetailView.vue";

const { fetchQuestion } = vi.hoisted(() => ({ fetchQuestion: vi.fn() }));

vi.mock("vue-router", () => ({ useRoute: () => ({ params: { questionId: "7" } }) }));
vi.mock("@/api/questions", async (importOriginal) => {
  const original = await importOriginal<typeof import("@/api/questions")>();
  return { ...original, fetchQuestion };
});

function mountView() {
  return mount(QuestionDetailView, {
    global: {
      stubs: {
        RouterLink: { template: "<a><slot /></a>" },
      },
    },
  });
}

describe("문제 상세 화면", () => {
  beforeEach(() => {
    fetchQuestion.mockReset();
  });

  it("공개 문제의 Topic과 난이도와 본문을 표시한다", async () => {
    fetchQuestion.mockResolvedValue({
      id: 7,
      topic: { id: 1, code: "OS", name: "운영체제" },
      difficulty: "INTERMEDIATE",
      content: "스레드와 프로세스의 차이는 무엇인가요?",
    });

    const wrapper = mountView();
    await flushPromises();

    expect(fetchQuestion).toHaveBeenCalledWith("7");
    expect(wrapper.text()).toContain("운영체제");
    expect(wrapper.text()).toContain("중급");
    expect(wrapper.text()).toContain("스레드와 프로세스의 차이는 무엇인가요?");
  });

  it("비공개이거나 존재하지 않는 문제는 같은 not found 상태를 표시한다", async () => {
    fetchQuestion.mockRejectedValue(new ApiClientError(404, "not found"));

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain("문제를 찾을 수 없어요");
    expect(wrapper.text()).toContain("존재하지 않거나 지금은 공개되지 않은 문제입니다.");
  });

  it("서버 오류가 발생하면 재시도 상태를 표시한다", async () => {
    fetchQuestion.mockRejectedValue(new ApiClientError(500, "server error"));

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain("문제를 불러오지 못했어요");
    expect(wrapper.get("button").text()).toBe("다시 불러오기");
  });
});
