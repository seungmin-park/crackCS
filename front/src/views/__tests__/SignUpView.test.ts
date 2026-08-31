import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "@/api/client";
import SignUpView from "@/views/SignUpView.vue";

const { signUp, push } = vi.hoisted(() => ({ signUp: vi.fn(), push: vi.fn() }));

vi.mock("@/api/auth", () => ({ signUp }));
vi.mock("vue-router", () => ({
  useRouter: () => ({ push }),
  RouterLink: { template: "<a><slot /></a>" },
}));

describe("회원가입 화면", () => {
  beforeEach(() => {
    signUp.mockReset();
    push.mockReset();
  });

  it("이메일과 비밀번호 및 닉네임으로 가입한 뒤 로그인 화면으로 이동한다", async () => {
    signUp.mockResolvedValue({ id: 1, nickname: "크랙러", role: "USER", status: "ACTIVE" });
    const wrapper = mount(SignUpView, {
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });

    await wrapper.get("input[name='email']").setValue("user@example.com");
    await wrapper.get("input[name='password']").setValue("correct horse battery staple");
    await wrapper.get("input[name='nickname']").setValue("크랙러");
    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(signUp).toHaveBeenCalledWith({
      email: "user@example.com",
      password: "correct horse battery staple",
      nickname: "크랙러",
    });
    expect(push).toHaveBeenCalledWith({ name: "login", query: { registered: "true" } });
  });

  it("서버 validation 오류를 각 입력 필드 아래에 표시한다", async () => {
    signUp.mockRejectedValue(new ApiClientError(400, "invalid", [
      { field: "email", reason: "올바른 이메일 형식이어야 합니다." },
      { field: "password", reason: "비밀번호 정책을 확인해 주세요." },
    ]));
    const wrapper = mount(SignUpView, {
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });

    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(wrapper.text()).toContain("올바른 이메일 형식이어야 합니다.");
    expect(wrapper.text()).toContain("비밀번호 정책을 확인해 주세요.");
    expect(wrapper.get("input[name='email']").attributes("aria-invalid")).toBe("true");
  });

  it("중복 이메일 오류는 양식 공통 오류로 표시한다", async () => {
    signUp.mockRejectedValue(new ApiClientError(409, "이미 가입된 이메일입니다."));
    const wrapper = mount(SignUpView, {
      global: { stubs: { RouterLink: { template: "<a><slot /></a>" } } },
    });

    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(wrapper.get("[role='alert']").text()).toBe("이미 가입된 이메일입니다.");
  });
});
