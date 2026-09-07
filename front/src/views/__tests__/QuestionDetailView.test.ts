import { reactive, ref } from "vue";
import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/api/client";
import QuestionDetailView from "@/views/QuestionDetailView.vue";

const { fetchQuestion, submitAnswer, push } = vi.hoisted(() => ({ fetchQuestion: vi.fn(), submitAnswer: vi.fn(), push: vi.fn() }));

const route = reactive({ params: { questionId: "7" }, query: {} });

const member = ref({ id: 41, role: "USER" });
vi.mock("@/composables/useAuth", () => ({ useAuth: () => ({ currentMember: member }) }));

vi.mock("vue-router", () => ({
  useRoute: () => route,
  useRouter: () => ({ push }),
}));
vi.mock("@/api/questions", async (importOriginal) => {
  const original = await importOriginal<typeof import("@/api/questions")>();
  return { ...original, fetchQuestion };
});
vi.mock("@/api/answers", () => ({ submitAnswer }));

function mountView() {
  return mount(QuestionDetailView, {
    global: {
      stubs: {
        RouterLink: { template: "<a><slot /></a>" },
      },
    },
  });
}

enableAutoUnmount(afterEach);

describe("문제 상세 화면", () => {
  beforeEach(() => {
    member.value = { id: 41, role: "USER" };
    route.params.questionId = "7";
    fetchQuestion.mockReset();
    submitAnswer.mockReset();
    push.mockReset();
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

  it("공백 답변은 막고 유효한 답변을 제출하면 답변 상세로 이동한다", async () => {
    fetchQuestion.mockResolvedValue({ id: 7, topic: { id: 1, code: "OS", name: "운영체제" }, difficulty: "BASIC", content: "질문" });
    submitAnswer.mockResolvedValue({ answerId: 31 });
    const wrapper = mountView();
    await flushPromises();

    const button = wrapper.get("button[type='submit']");
    expect(button.attributes("disabled")).toBeDefined();
    await wrapper.get("textarea").setValue("프로세스는 자원을 소유합니다.");
    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(push).toHaveBeenCalledWith({ name: "answer-detail", params: { answerId: 31 } });
  });
  it("같은 화면에서 문제 ID가 바뀌면 새 문제를 불러오고 새 문제에 제출한다", async () => {
    fetchQuestion.mockImplementation(async (id: string) => ({ id: Number(id), topic: { id: 1, code: "OS", name: "운영체제" }, difficulty: "BASIC", content: `질문 ${id}` }));
    submitAnswer.mockResolvedValue({ answerId: 32 });
    const wrapper = mountView();
    await flushPromises();
    route.params.questionId = "8";
    await flushPromises();
    expect(wrapper.text()).toContain("질문 8");
    await wrapper.get("textarea").setValue("새 문제 답변");
    await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(submitAnswer).toHaveBeenLastCalledWith(8, expect.objectContaining({ content: "새 문제 답변" }));
    wrapper.unmount();
  });

  it("관리자는 문제를 조회해도 학습자 답변 폼을 표시하지 않는다", async () => {
    member.value = { id: 41, role: "ADMIN" };
    fetchQuestion.mockResolvedValue({ id: 7, topic: { id: 1, code: "OS", name: "운영체제" }, difficulty: "BASIC", content: "질문" });
    const wrapper = mountView();
    await flushPromises();
    expect(wrapper.find("form.answer-form").exists()).toBe(false);
  });

});
