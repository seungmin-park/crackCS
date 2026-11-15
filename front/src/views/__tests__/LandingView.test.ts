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

it("소개 문구의 단어 사이 공백을 줄바꿈 위치와 관계없이 유지한다", () => {
  vi.stubGlobal("matchMedia", () => ({ matches: true }));
  const wrapper = mount(LandingView, { global: { stubs: { RouterLink: true } } });

  expect(wrapper.find(".tagline h2").text()).toBe("외운 문장보다,내가 설명한 한 문장.");
});

it("공개한 문제 규모와 평가 주체를 소개 화면에서 알 수 있다", () => {
  vi.stubGlobal("matchMedia", () => ({ matches: true }));
  const wrapper = mount(LandingView, { global: { stubs: { RouterLink: true } } });

  expect(wrapper.find(".proof-strip").text()).toContain("25개 주제 260여 문항");
  expect(wrapper.text()).toContain("참고 문서를 바탕으로 AI가 개념별 판정과 평가 근거를 제공해요.");
});
