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

it("학습 흐름은 한 번만 설명하고 부족한 개념은 후속 질문으로 이어진다고 안내한다", () => {
  vi.stubGlobal("matchMedia", () => ({ matches: true }));
  const wrapper = mount(LandingView, { global: { stubs: { RouterLink: true } } });

  expect(wrapper.findAll(".benefit")).toHaveLength(3);
  expect(wrapper.find(".steps-grid").exists()).toBe(false);
  expect(wrapper.find(".hero-lede").text()).toContain("부족한 개념은 후속 질문으로 다시 풀어 보세요.");
  expect(wrapper.find(".final-cta h2").text()).toBe("지금 아는 만큼, 첫 문장을 써 보세요.");
});

it("구호형 대구는 첫 제목과 가운데 문장에만 쓴다", () => {
  vi.stubGlobal("matchMedia", () => ({ matches: true }));
  const wrapper = mount(LandingView, { global: { stubs: { RouterLink: true } } });
  const text = wrapper.text();

  expect(text).toContain("설명할 수 있는 CS로.");
  expect(wrapper.find(".tagline h2").text()).toBe("외운 문장보다,내가 설명한 한 문장.");
  for (const slogan of ["읽는 공부에서, 설명하는 공부로", "막연한 이해에서, 분명한 설명으로", "점수보다 중요한, 그다음 이해", "내일의 분명한 설명이 되도록"]) {
    expect(text).not.toContain(slogan);
  }
});
