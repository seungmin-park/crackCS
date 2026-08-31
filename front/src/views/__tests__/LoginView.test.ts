import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/api/client";
import LoginView from "@/views/LoginView.vue";

const { login, push } = vi.hoisted(() => ({ login: vi.fn(), push: vi.fn() }));
let query: Record<string, string> = {};

vi.mock("@/composables/useAuth", () => ({ useAuth: () => ({ login }) }));
vi.mock("vue-router", () => ({
  useRoute: () => ({ query }),
  useRouter: () => ({ push }),
}));

describe("로그인 화면", () => {
  beforeEach(() => {
    login.mockReset();
    push.mockReset();
    query = {};
  });

  it("로그인에 성공하면 원래 요청한 보호 화면으로 이동한다", async () => {
    query = { redirect: "/questions/7" };
    login.mockResolvedValue({ id: 1, nickname: "크랙러", role: "USER", status: "ACTIVE" });
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });

    await wrapper.get("input[name='email']").setValue("user@example.com");
    await wrapper.get("input[name='password']").setValue("correct horse battery staple");
    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(login).toHaveBeenCalledWith({
      email: "user@example.com",
      password: "correct horse battery staple",
    });
    expect(push).toHaveBeenCalledWith("/questions/7");
  });

  it("가입 직후에는 계정 생성 완료 안내를 표시한다", () => {
    query = { registered: "true" };

    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });

    expect(wrapper.get("[role='status']").text()).toContain("계정이 만들어졌습니다");
  });

  it("인증 실패는 계정 존재 여부를 구분하지 않는 메시지로 표시한다", async () => {
    login.mockRejectedValue(new ApiClientError(401, "이메일 또는 비밀번호가 올바르지 않습니다."));
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });

    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(wrapper.get("[role='alert']").text()).toBe("이메일 또는 비밀번호가 올바르지 않습니다.");
  });
});
