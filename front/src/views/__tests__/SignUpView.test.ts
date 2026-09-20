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

  it("가입 처리 중 중복 제출을 무시하고 최초 입력 스냅샷을 보낸다", async () => {
    let resolveSignUp!: (member: { id: number; nickname: string; role: "USER"; status: "ACTIVE" }) => void;
    signUp.mockReturnValue(new Promise((resolve) => { resolveSignUp = resolve; }));
    const wrapper = mount(SignUpView, { global: { stubs: { RouterLink: true } } });
    await wrapper.get("input[name='email']").setValue("first@example.com");
    await wrapper.get("input[name='password']").setValue("first-password-value");
    await wrapper.get("input[name='nickname']").setValue("처음닉네임");

    await wrapper.get("form").trigger("submit");
    await wrapper.get("input[name='email']").setValue("changed@example.com");
    await wrapper.get("input[name='nickname']").setValue("바뀐닉네임");
    await wrapper.get("form").trigger("submit");

    expect(signUp).toHaveBeenCalledOnce();
    expect(signUp).toHaveBeenCalledWith({
      email: "first@example.com",
      password: "first-password-value",
      nickname: "처음닉네임",
    });
    resolveSignUp({ id: 1, nickname: "처음닉네임", role: "USER", status: "ACTIVE" });
    await flushPromises();
  });

  it("화면을 떠난 뒤 끝난 가입은 로그인 화면으로 이동하지 않는다", async () => {
    let resolveSignUp!: (member: { id: number; nickname: string; role: "USER"; status: "ACTIVE" }) => void;
    signUp.mockReturnValue(new Promise((resolve) => { resolveSignUp = resolve; }));
    const wrapper = mount(SignUpView, { global: { stubs: { RouterLink: true } } });

    await wrapper.get("form").trigger("submit");
    wrapper.unmount();
    resolveSignUp({ id: 1, nickname: "학습자", role: "USER", status: "ACTIVE" });
    await flushPromises();

    expect(push).not.toHaveBeenCalled();
  });

  it("화면을 떠난 뒤 실패한 가입은 이전 화면의 오류 상태를 바꾸지 않는다", async () => {
    let rejectSignUp!: (error: Error) => void;
    signUp.mockReturnValue(new Promise((_resolve, reject) => { rejectSignUp = reject; }));
    const wrapper = mount(SignUpView, { global: { stubs: { RouterLink: true } } });
    const setupState = wrapper.vm.$.setupState as { generalError: string };

    await wrapper.get("form").trigger("submit");
    wrapper.unmount();
    rejectSignUp(new Error("late failure"));
    await flushPromises();

    expect(setupState.generalError).toBe("");
    expect(push).not.toHaveBeenCalled();
  });
});
