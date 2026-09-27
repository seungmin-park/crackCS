import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ConnectionErrorView from "@/views/ConnectionErrorView.vue";

const { route, replace } = vi.hoisted(() => ({ route: { query: {} as Record<string, unknown> }, replace: vi.fn() }));
vi.mock("vue-router", () => ({ useRoute: () => route, useRouter: () => ({ replace }) }));
enableAutoUnmount(afterEach);

describe("연결 복구 화면", () => {
  beforeEach(() => { route.query = {}; replace.mockReset(); });
  it("원래 주소로 재시도하는 동안 중복 클릭을 막고 실패 뒤 다시 연결할 수 있다", async () => {
    route.query.redirect = "/questions?page=2";
    let reject!: (error: Error) => void;
    replace.mockReturnValueOnce(new Promise((_resolve, fail) => { reject = fail; }));
    const wrapper = mount(ConnectionErrorView);
    await wrapper.get("button").trigger("click");
    expect(replace).toHaveBeenCalledExactlyOnceWith("/questions?page=2");
    expect(wrapper.find("button").exists()).toBe(false);
    expect(wrapper.get('[aria-busy="true"]').exists()).toBe(true);
    reject(new Error("offline"));
    await flushPromises();
    await wrapper.get("button").trigger("click");
    expect(replace).toHaveBeenCalledTimes(2);
  });
  it.each(["https://example.com", "//example.com", "/connection-error", ["/questions"]])("잘못된 복귀 주소 %s는 홈으로 이동한다", async redirect => {
    route.query.redirect = redirect;
    const wrapper = mount(ConnectionErrorView);
    await wrapper.get("button").trigger("click");
    expect(replace).toHaveBeenCalledWith("/");
  });
});
