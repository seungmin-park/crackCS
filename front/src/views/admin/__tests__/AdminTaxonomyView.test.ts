import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

const api = vi.hoisted(() => ({
  fetchTopics: vi.fn(), fetchConcepts: vi.fn(), createTopic: vi.fn(), updateTopic: vi.fn(),
  deactivateTopic: vi.fn(), createConcept: vi.fn(), updateConcept: vi.fn(), deactivateConcept: vi.fn(),
}));

vi.mock("@/api/admin", async (importOriginal) => ({ ...(await importOriginal<typeof import("@/api/admin")>()), ...api }));

import AdminTaxonomyView from "@/views/admin/AdminTaxonomyView.vue";

describe("관리자 Topic과 Concept 화면", () => {
  beforeEach(() => {
    Object.values(api).forEach(mock => mock.mockReset());
    api.fetchTopics.mockResolvedValue({ content: [{ id: 1, parentId: null, code: "OS", name: "운영체제", active: true }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    api.fetchConcepts.mockResolvedValue({ content: [{ id: 2, topicId: 1, code: "THREAD", name: "스레드", description: null, active: true }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
  });

  it("Topic과 Concept 목록을 함께 표시한다", async () => {
    const wrapper = mount(AdminTaxonomyView);
    await flushPromises();
    expect(wrapper.text()).toContain("운영체제");
    expect(wrapper.text()).toContain("스레드");
  });

  it("Topic 등록 성공 후 공통 성공 피드백을 표시하고 목록을 다시 조회한다", async () => {
    api.createTopic.mockResolvedValue({ id: 3, parentId: null, code: "NETWORK", name: "네트워크", active: true });
    const wrapper = mount(AdminTaxonomyView);
    await flushPromises();
    const form = wrapper.findAll("form")[0]!;
    await form.findAll("input")[0]!.setValue("NETWORK");
    await form.findAll("input")[1]!.setValue("네트워크");
    await form.trigger("submit");
    await flushPromises();
    expect(api.createTopic).toHaveBeenCalledWith({ code: "NETWORK", name: "네트워크" });
    expect(wrapper.text()).toContain("Topic을 등록했습니다.");
    expect(api.fetchTopics).toHaveBeenCalledTimes(2);
  });
});
