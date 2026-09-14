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
  it("지정한 경로가 없으면 학습 홈으로 이동한다", async () => {
    login.mockResolvedValue({ id: 1, nickname: "학습자", role: "USER", status: "ACTIVE" });
    const wrapper = mount(LoginView, { global: { stubs: { RouterLink: true } } });
    await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(push).toHaveBeenCalledWith("/");
    wrapper.unmount();
  });

  it("ADMIN 로그인 기본 경로는 관리자 홈이다", async () => {
    login.mockResolvedValue({ id: 2, nickname: "관리자", role: "ADMIN", status: "ACTIVE" });
    const wrapper = mount(LoginView, { global: { stubs: { RouterLink: true } } });
    await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(push).toHaveBeenCalledWith("/admin");
    wrapper.unmount();
  });
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

  it("외부 또는 프로토콜 상대 redirect는 무시하고 역할 기본 화면으로 이동한다", async () => {
    query = { redirect: "//evil.example/steal" };
    login.mockResolvedValue({ id: 1, nickname: "학습자", role: "USER", status: "ACTIVE" });
    const wrapper = mount(LoginView, { global: { stubs: { RouterLink: true } } });

    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(push).toHaveBeenCalledWith("/");
  });

  it("로그인 처리 중 중복 제출을 무시하고 최초 입력 스냅샷을 보낸다", async () => {
    let resolveLogin!: (member: { id: number; nickname: string; role: "USER"; status: "ACTIVE" }) => void;
    login.mockReturnValue(new Promise((resolve) => { resolveLogin = resolve; }));
    const wrapper = mount(LoginView, { global: { stubs: { RouterLink: true } } });
    await wrapper.get("input[name='email']").setValue("first@example.com");
    await wrapper.get("input[name='password']").setValue("first-password");

    await wrapper.get("form").trigger("submit");
    await wrapper.get("input[name='email']").setValue("changed@example.com");
    await wrapper.get("form").trigger("submit");

    expect(login).toHaveBeenCalledOnce();
    expect(login).toHaveBeenCalledWith({ email: "first@example.com", password: "first-password" });
    resolveLogin({ id: 1, nickname: "학습자", role: "USER", status: "ACTIVE" });
    await flushPromises();
  });

  it("화면을 떠난 뒤 끝난 로그인은 이전 화면에서 이동을 실행하지 않는다", async () => {
    let resolveLogin!: (member: { id: number; nickname: string; role: "USER"; status: "ACTIVE" }) => void;
    login.mockReturnValue(new Promise((resolve) => { resolveLogin = resolve; }));
    const wrapper = mount(LoginView, { global: { stubs: { RouterLink: true } } });

    await wrapper.get("form").trigger("submit");
    wrapper.unmount();
    resolveLogin({ id: 1, nickname: "학습자", role: "USER", status: "ACTIVE" });
    await flushPromises();

    expect(push).not.toHaveBeenCalled();
  });
});
