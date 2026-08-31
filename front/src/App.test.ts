import { mount } from "@vue/test-utils";
import { computed, ref } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";

const member = ref<{ nickname: string; role: "USER" | "ADMIN" } | null>(null);
const restoreAuthentication = vi.fn();
const logout = vi.fn();

vi.mock("@/composables/useAuth", () => ({
  useAuth: () => ({
    currentMember: computed(() => member.value),
    restoreAuthentication,
    logout,
  }),
}));
vi.mock("vue-router", async (importOriginal) => {
  const original = await importOriginal<typeof import("vue-router")>();
  return { ...original, useRouter: () => ({ push: vi.fn() }) };
});

import App from "@/App.vue";

describe("애플리케이션 헤더", () => {
  beforeEach(() => {
    member.value = null;
    restoreAuthentication.mockReset();
    restoreAuthentication.mockResolvedValue(undefined);
  });

  it("USER에게는 관리자 링크를 표시하지 않는다", () => {
    member.value = { nickname: "일반 회원", role: "USER" };

    const wrapper = mount(App, {
      global: {
        stubs: {
          RouterLink: { template: "<a><slot /></a>" },
          RouterView: true,
        },
      },
    });

    expect(wrapper.text()).toContain("일반 회원");
    expect(wrapper.text()).not.toContain("관리");
  });

  it("ADMIN에게 관리자 링크를 로그아웃 바로 왼쪽에 표시한다", () => {
    member.value = { nickname: "관리자", role: "ADMIN" };

    const wrapper = mount(App, {
      global: {
        stubs: {
          RouterLink: { template: "<a><slot /></a>" },
          RouterView: true,
        },
      },
    });

    expect(wrapper.findAll("nav > *").map((element) => element.text())).toEqual([
      "관리자",
      "관리",
      "로그아웃",
    ]);
  });
});
