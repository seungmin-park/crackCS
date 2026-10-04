import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import ThemeSwitch from "./ThemeSwitch.vue";

enableAutoUnmount(afterEach);

describe("테마 선택 메뉴", () => {
  let media: EventTarget & { matches: boolean };

  beforeEach(() => {
    const saved = new Map<string, string>();
    vi.stubGlobal("localStorage", {
      getItem: (key: string) => saved.get(key) ?? null,
      setItem: (key: string, value: string) => saved.set(key, value),
    });
    media = Object.assign(new EventTarget(), { matches: false });
    vi.stubGlobal("matchMedia", () => media);
  });

  afterEach(() => {
    delete document.documentElement.dataset.theme;
    vi.unstubAllGlobals();
  });

  it("현재 테마 버튼을 열면 세 가지 선택지와 선택 상태를 표시한다", async () => {
    const wrapper = mount(ThemeSwitch, { attachTo: document.body });

    expect(wrapper.find('button[aria-haspopup="menu"]').exists()).toBe(true);
    const trigger = wrapper.get('button[aria-haspopup="menu"]');
    expect(trigger.attributes("aria-expanded")).toBe("false");
    expect(wrapper.find('[role="menu"]').exists()).toBe(false);

    await trigger.trigger("click");

    expect(trigger.attributes("aria-expanded")).toBe("true");
    expect(wrapper.findAll('[role="menuitemradio"]').map(option => option.text())).toEqual(["시스템", "화이트", "다크"]);
    expect(wrapper.get('[data-theme-choice="system"]').attributes("aria-checked")).toBe("true");
  });

  it("다크를 선택하면 적용하고 저장한 뒤 메뉴를 닫는다", async () => {
    const wrapper = mount(ThemeSwitch, { attachTo: document.body });
    await wrapper.get('button[aria-haspopup="menu"]').trigger("click");

    await wrapper.get('[data-theme-choice="dark"]').trigger("click");

    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(localStorage.getItem("crackcs-theme")).toBe("dark");
    expect(wrapper.find('[role="menu"]').exists()).toBe(false);
    expect(wrapper.get('button[aria-haspopup="menu"]').text()).toBe("다크");
  });

  it("Escape를 누르면 테마를 바꾸지 않고 버튼으로 포커스를 돌려준다", async () => {
    const wrapper = mount(ThemeSwitch, { attachTo: document.body });
    const trigger = wrapper.get('button[aria-haspopup="menu"]');
    await trigger.trigger("click");

    await wrapper.get('[data-theme-choice="system"]').trigger("keydown", { key: "Escape" });

    expect(wrapper.find('[role="menu"]').exists()).toBe(false);
    expect(document.activeElement).toBe(trigger.element);
    expect(document.documentElement.dataset.theme).toBe("light");
    expect(localStorage.getItem("crackcs-theme")).toBeNull();
  });

  it.each([
    { key: "ArrowDown", from: "system", target: "light" },
    { key: "ArrowUp", from: "system", target: "dark" },
    { key: "Home", from: "dark", target: "system" },
    { key: "End", from: "system", target: "dark" },
  ])("$key 키로 선택 위치만 이동하고 테마는 유지한다", async ({ key, from, target }) => {
    media.matches = true;
    const wrapper = mount(ThemeSwitch, { attachTo: document.body });
    await wrapper.get('button[aria-haspopup="menu"]').trigger("click");
    const currentOption = wrapper.get(`[data-theme-choice="${from}"]`);
    (currentOption.element as HTMLButtonElement).focus();

    await currentOption.trigger("keydown", { key });

    expect(document.activeElement).toBe(wrapper.get(`[data-theme-choice="${target}"]`).element);
    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(localStorage.getItem("crackcs-theme")).toBeNull();
    expect(wrapper.get('[data-theme-choice="system"]').attributes("aria-checked")).toBe("true");
  });

  it.each(["Enter", " "])("%s 키로 현재 선택지를 확정한다", async key => {
    const wrapper = mount(ThemeSwitch, { attachTo: document.body });
    await wrapper.get('button[aria-haspopup="menu"]').trigger("click");
    const darkOption = wrapper.get('[data-theme-choice="dark"]');
    (darkOption.element as HTMLButtonElement).focus();

    await darkOption.trigger("keydown", { key });

    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(localStorage.getItem("crackcs-theme")).toBe("dark");
    expect(wrapper.find('[role="menu"]').exists()).toBe(false);
  });

  it("바깥을 누르면 메뉴를 닫고 누른 곳의 포커스를 유지한다", async () => {
    const wrapper = mount(ThemeSwitch, { attachTo: document.body });
    await wrapper.get('button[aria-haspopup="menu"]').trigger("click");
    const outsideButton = document.createElement("button");
    document.body.append(outsideButton);
    try {
      outsideButton.focus();

      outsideButton.dispatchEvent(new MouseEvent("pointerdown", { bubbles: true }));
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[role="menu"]').exists()).toBe(false);
      expect(document.activeElement).toBe(outsideButton);
      expect(localStorage.getItem("crackcs-theme")).toBeNull();
    } finally {
      outsideButton.remove();
    }
  });

  it("Tab으로 이동할 때 메뉴를 닫아 다음 컨트롤로 나갈 수 있다", async () => {
    const wrapper = mount(ThemeSwitch, { attachTo: document.body });
    await wrapper.get('button[aria-haspopup="menu"]').trigger("click");

    await wrapper.get('[data-theme-choice="system"]').trigger("keydown", { key: "Tab" });

    expect(wrapper.find('[role="menu"]').exists()).toBe(false);
    expect(localStorage.getItem("crackcs-theme")).toBeNull();
  });

  it("저장된 다크에서 시스템으로 전환하면 운영체제 설정을 다시 따른다", async () => {
    localStorage.setItem("crackcs-theme", "dark");
    const wrapper = mount(ThemeSwitch, { attachTo: document.body });
    await wrapper.get('button[aria-haspopup="menu"]').trigger("click");

    await wrapper.get('[data-theme-choice="system"]').trigger("click");

    expect(document.documentElement.dataset.theme).toBe("light");
    expect(localStorage.getItem("crackcs-theme")).toBe("system");
    media.matches = true;
    media.dispatchEvent(new Event("change"));
    expect(document.documentElement.dataset.theme).toBe("dark");
  });
});
