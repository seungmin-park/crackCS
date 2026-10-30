import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, expect, it, vi } from "vitest";
import LandingView from "@/views/LandingView.vue";

enableAutoUnmount(afterEach);
afterEach(() => vi.unstubAllGlobals());

it("화면을 떠나면 소개 문구를 관찰하던 observer를 해제한다", () => {
  const observe = vi.fn();
  const disconnect = vi.fn();
  vi.stubGlobal("IntersectionObserver", class { observe = observe; disconnect = disconnect; });
  vi.stubGlobal("matchMedia", () => ({ matches: false }));
  const wrapper = mount(LandingView, { global: { stubs: { RouterLink: true } } });

  expect(observe).toHaveBeenCalled();
  wrapper.unmount();
  expect(disconnect).toHaveBeenCalledTimes(2);
});

it("동작 줄이기를 선택하면 소개 문구를 숨기거나 관찰하지 않는다", () => {
  const observer = vi.fn();
  vi.stubGlobal("IntersectionObserver", observer);
  vi.stubGlobal("matchMedia", () => ({ matches: true }));
  const wrapper = mount(LandingView, { global: { stubs: { RouterLink: true } } });

  expect(observer).not.toHaveBeenCalled();
  expect(wrapper.find(".is-observed").exists()).toBe(false);
  expect(wrapper.text()).toContain("설명할 수 있는 CS로.");
});
