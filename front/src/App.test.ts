import { enableAutoUnmount, mount } from "@vue/test-utils";
import { computed, ref } from "vue";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

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

enableAutoUnmount(afterEach);

describe("화면 테마", () => {
  let media: EventTarget & { matches: boolean };
  const mountApp = () => mount(App, { global: { stubs: { RouterLink: { template: "<a><slot /></a>" }, RouterView: true } } });

  beforeEach(() => {
    const saved = new Map<string, string>();
    vi.stubGlobal("localStorage", {
      getItem: (key: string) => saved.get(key) ?? null,
      setItem: (key: string, value: string) => saved.set(key, value),
      clear: () => saved.clear(),
    });
    media = Object.assign(new EventTarget(), { matches: false });
    vi.stubGlobal("matchMedia", () => media);
  });

  afterEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset.theme;
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("처음에는 시스템의 다크 설정을 따른다", () => {
    media.matches = true;
    const wrapper = mountApp();
    expect(wrapper.find('select[aria-label="화면 테마"]').exists()).toBe(true);
    expect(document.documentElement.dataset.theme).toBe("dark");
  });

  it("선택한 다크 모드를 적용하고 다음 방문을 위해 저장한다", async () => {
    const wrapper = mountApp();
    await wrapper.get('select[aria-label="화면 테마"]').setValue("dark");
    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(localStorage.getItem("crackcs-theme")).toBe("dark");
  });

  it("시스템이 다크여도 저장한 화이트 모드를 복구한다", () => {
    localStorage.setItem("crackcs-theme", "light");
    media.matches = true;
    const wrapper = mountApp();
    expect(wrapper.find('select[aria-label="화면 테마"]').exists()).toBe(true);
    expect(document.documentElement.dataset.theme).toBe("light");
  });

  it("시스템 설정을 선택한 동안 운영체제 변경을 반영한다", async () => {
    const wrapper = mountApp();
    expect(wrapper.find('select[aria-label="화면 테마"]').exists()).toBe(true);
    media.matches = true;
    media.dispatchEvent(new Event("change"));
    expect(document.documentElement.dataset.theme).toBe("dark");
    await wrapper.get('select[aria-label="화면 테마"]').setValue("light");
    media.dispatchEvent(new Event("change"));
    expect(document.documentElement.dataset.theme).toBe("light");
  });

  it("저장이 차단되어도 화면 테마는 전환된다", async () => {
    vi.spyOn(localStorage, "getItem").mockImplementation(() => { throw new Error("blocked"); });
    vi.spyOn(localStorage, "setItem").mockImplementation(() => { throw new Error("blocked"); });
    const wrapper = mountApp();
    await wrapper.get('select[aria-label="화면 테마"]').setValue("dark");
    expect(document.documentElement.dataset.theme).toBe("dark");
  });
});

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
