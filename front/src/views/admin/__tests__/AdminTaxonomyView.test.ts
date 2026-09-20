import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { reactive } from "vue";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
enableAutoUnmount(afterEach);
const route = reactive({ query: {} as Record<string, string> });
const push = vi.fn(async ({ query }: { query: Record<string, string> }) => { route.query = query; });
const replace = vi.fn(async ({ query }: { query: Record<string, string> }) => { route.query = query; });
vi.mock("vue-router", () => ({ useRoute: () => route, useRouter: () => ({ push, replace }) }));

const api = vi.hoisted(() => ({
  fetchTopics: vi.fn(), fetchConcepts: vi.fn(), createTopic: vi.fn(), updateTopic: vi.fn(),
  deactivateTopic: vi.fn(), createConcept: vi.fn(), updateConcept: vi.fn(), deactivateConcept: vi.fn(),
}));

vi.mock("@/api/admin/topics", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/api/admin/topics")>()),
  fetchTopics: api.fetchTopics,
  createTopic: api.createTopic,
  updateTopic: api.updateTopic,
  deactivateTopic: api.deactivateTopic,
}));
vi.mock("@/api/admin/concepts", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/api/admin/concepts")>()),
  fetchConcepts: api.fetchConcepts,
  createConcept: api.createConcept,
  updateConcept: api.updateConcept,
  deactivateConcept: api.deactivateConcept,
}));

import AdminTaxonomyView from "@/views/admin/AdminTaxonomyView.vue";

describe("관리자 Topic과 Concept 화면", () => {
  beforeEach(() => {
    Object.values(api).forEach(mock => mock.mockReset());
    route.query = {}; push.mockClear(); replace.mockClear();
    api.fetchTopics.mockResolvedValue({ content: [{ id: 1, parentId: null, code: "OS", name: "운영체제", active: true }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    api.fetchConcepts.mockResolvedValue({ content: [{ id: 2, topicId: 1, code: "THREAD", name: "스레드", description: null, active: true }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
  });

  it.each([
    ["createTopic", 0, "submit"],
    ["updateTopic", 0, "edit"],
    ["deactivateTopic", 0, "deactivate"],
    ["createConcept", 1, "submit"],
    ["updateConcept", 1, "edit"],
    ["deactivateConcept", 1, "deactivate"],
  ] as const)("%s 대기 중 화면을 떠나면 목록과 관계 후보를 다시 조회하지 않는다", async (method, panelIndex, action) => {
    let resolveMutation!: (value?: object) => void;
    api[method].mockReturnValue(new Promise(resolve => { resolveMutation = resolve; }));
    const wrapper = mount(AdminTaxonomyView);
    await flushPromises();
    const panel = wrapper.findAll(".admin-panel")[panelIndex]!;
    if (action === "deactivate") {
      await panel.findAll("button").find(button => button.text() === "비활성화")!.trigger("click");
    } else {
      if (action === "edit") await panel.findAll("button").find(button => button.text() === "편집")!.trigger("click");
      await panel.get("form").trigger("submit");
    }
    expect(api[method]).toHaveBeenCalledOnce();
    wrapper.unmount();
    route.query = { page: "3", status: "ACTIVE" };
    api.fetchTopics.mockClear();
    api.fetchConcepts.mockClear();
    resolveMutation(action === "deactivate" ? undefined : { id: 3 });
    await flushPromises();
    expect(api.fetchTopics).not.toHaveBeenCalled();
    expect(api.fetchConcepts).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
    expect(route.query).toEqual({ page: "3", status: "ACTIVE" });
  });

  it("진행 중 분류 목록 응답은 화면 폐기 후 목적지 URL을 교정하지 않는다", async () => {
    let resolveList!: (value: object) => void;
    route.query = { topicPage: "1", conceptPage: "1" };
    api.fetchTopics.mockReturnValueOnce(new Promise(resolve => { resolveList = resolve; }));
    const wrapper = mount(AdminTaxonomyView);
    await flushPromises();
    wrapper.unmount();
    route.query = { page: "3", status: "ACTIVE" };
    resolveList({ content: [], page: 1, size: 20, totalPages: 0, totalElements: 0 });
    await flushPromises();
    expect(replace).not.toHaveBeenCalled();
    expect(route.query).toEqual({ page: "3", status: "ACTIVE" });
  });

  it("Topic과 Concept의 범위 밖 page를 한 번에 서로 다른 query key로 replace한다", async () => {
    route.query = { topicPage: "4", conceptPage: "3" };
    api.fetchTopics.mockResolvedValueOnce({ content: [], page: 4, size: 20, totalElements: 30, totalPages: 2 });
    api.fetchConcepts.mockResolvedValueOnce({ content: [], page: 3, size: 20, totalElements: 0, totalPages: 0 });
    mount(AdminTaxonomyView);
    await flushPromises();
    expect(replace).toHaveBeenCalledWith({ query: { topicPage: "1", conceptPage: "0" } });
  });

  it("Topic과 Concept의 서로 다른 URL page를 각각 조회한다", async () => {
    route.query = { topicPage: "2", conceptPage: "3" };
    api.fetchTopics.mockResolvedValueOnce({ content: [], page: 2, size: 20, totalElements: 50, totalPages: 3 });
    api.fetchConcepts.mockResolvedValueOnce({ content: [], page: 3, size: 20, totalElements: 80, totalPages: 4 });
    const wrapper = mount(AdminTaxonomyView);
    await flushPromises();
    expect(api.fetchTopics).toHaveBeenCalledWith({ page: 2, size: 20 });
    expect(api.fetchConcepts).toHaveBeenCalledWith({ page: 3, size: 20 });
    await wrapper.get("[data-page-key='topicPage'] button[data-page='previous']").trigger("click");
    expect(push).toHaveBeenCalledWith({ query: { topicPage: "1", conceptPage: "3" } });
  });

  it("브라우저 이동으로 taxonomy query가 바뀌면 두 목록을 새 page로 조회한다", async () => {
    const wrapper = mount(AdminTaxonomyView);
    await flushPromises(); api.fetchTopics.mockClear(); api.fetchConcepts.mockClear();
    route.query = { topicPage: "1", conceptPage: "2" };
    await flushPromises();
    expect(api.fetchTopics).toHaveBeenCalledWith({ page: 1, size: 20 });
    expect(api.fetchConcepts).toHaveBeenCalledWith({ page: 2, size: 20 });
    wrapper.unmount();
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
    expect(api.fetchTopics.mock.calls.filter(([request]) => request.size === 20)).toHaveLength(2);
  });

  it("Topic 등록 성공 후 관계 후보를 갱신해 새 Topic을 선택할 수 있다", async () => {
    let relationTopics = [{ id: 1, parentId: null, code: "OS", name: "운영체제", active: true }];
    api.fetchTopics.mockImplementation(async request => ({
      content: request.active ? relationTopics : relationTopics,
      page: request.page ?? 0, size: request.size ?? 20, totalElements: relationTopics.length, totalPages: 1,
    }));
    api.createTopic.mockImplementation(async () => {
      const created = { id: 3, parentId: null, code: "NETWORK", name: "네트워크", active: true };
      relationTopics = [...relationTopics, created];
      return created;
    });
    const wrapper = mount(AdminTaxonomyView);
    await flushPromises();
    const form = wrapper.findAll("form")[0]!;
    await form.findAll("input")[0]!.setValue("NETWORK");
    await form.findAll("input")[1]!.setValue("네트워크");
    await form.trigger("submit");
    await flushPromises();

    expect(wrapper.findAll("form")[0]!.find("select").text()).toContain("네트워크");
  });

  it("Topic 비활성화 성공 후 관계 후보에서 제거한다", async () => {
    let relationTopics = [{ id: 1, parentId: null, code: "OS", name: "운영체제", active: true }];
    api.fetchTopics.mockImplementation(async request => ({
      content: relationTopics,
      page: request.page ?? 0, size: request.size ?? 20, totalElements: relationTopics.length, totalPages: 1,
    }));
    api.deactivateTopic.mockImplementation(async () => { relationTopics = [{ ...relationTopics[0]!, active: false }]; });
    const wrapper = mount(AdminTaxonomyView);
    await flushPromises();
    expect(wrapper.findAll("form")[1]!.find("select").text()).toContain("운영체제");

    await wrapper.findAll("button").find(button => button.text() === "비활성화")!.trigger("click");
    await flushPromises();

    expect(wrapper.findAll("form")[1]!.find("select").text()).not.toContain("운영체제");
  });
});
