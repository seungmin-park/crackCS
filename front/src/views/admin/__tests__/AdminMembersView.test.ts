import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { reactive } from "vue";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
enableAutoUnmount(afterEach);
const route = reactive({ query: {} as Record<string, string> });
const push = vi.fn(async ({ query }: { query: Record<string, string> }) => { route.query = query; });
const replace = vi.fn(async ({ query }: { query: Record<string, string> }) => { route.query = query; });
vi.mock("vue-router", () => ({ useRoute: () => route, useRouter: () => ({ push, replace }) }));

const api = vi.hoisted(() => ({ fetchAdminMembers: vi.fn(), updateMemberStatus: vi.fn() }));
vi.mock("@/api/admin/members", async importOriginal => ({ ...(await importOriginal<typeof import("@/api/admin/members")>()), ...api }));
import AdminMembersView from "@/views/admin/AdminMembersView.vue";

describe("관리자 회원 화면", () => {
  beforeEach(() => {
    Object.values(api).forEach(mock => mock.mockReset());
    route.query = {}; push.mockClear(); replace.mockClear();
    api.fetchAdminMembers.mockResolvedValue({ content: [{ id: 1, nickname: "회원", role: "USER", status: "ACTIVE" }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
  });

  it("상태 변경 대기 중 화면을 떠나면 목록을 다시 조회하거나 목적지 URL을 바꾸지 않는다", async () => {
    let resolveMutation!: (value: object) => void;
    api.updateMemberStatus.mockReturnValue(new Promise(resolve => { resolveMutation = resolve; }));
    route.query = { page: "1", status: "ACTIVE" };
    api.fetchAdminMembers.mockResolvedValueOnce({
      content: [{ id: 1, nickname: "회원", role: "USER", status: "ACTIVE" }],
      page: 1, size: 20, totalElements: 21, totalPages: 2,
    });
    const wrapper = mount(AdminMembersView);
    await flushPromises();
    await wrapper.get(".admin-list select").setValue("BLOCKED");
    expect(api.updateMemberStatus).toHaveBeenCalledOnce();
    wrapper.unmount();
    route.query = { page: "3", status: "DRAFT" };
    api.fetchAdminMembers.mockClear();
    resolveMutation({ id: 1, status: "BLOCKED" });
    await flushPromises();
    expect(api.fetchAdminMembers).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
    expect(route.query).toEqual({ page: "3", status: "DRAFT" });
  });

  it("화면이 유지되면 상태 변경 성공 뒤 회원 목록을 갱신한다", async () => {
    api.updateMemberStatus.mockResolvedValue({ id: 1, status: "BLOCKED" });
    const wrapper = mount(AdminMembersView);
    await flushPromises();
    api.fetchAdminMembers.mockResolvedValue({
      content: [{ id: 1, nickname: "회원", role: "USER", status: "BLOCKED" }],
      page: 0, size: 20, totalElements: 1, totalPages: 1,
    });
    await wrapper.get(".admin-list select").setValue("BLOCKED");
    await flushPromises();
    expect(api.fetchAdminMembers).toHaveBeenCalledTimes(2);
    expect(wrapper.get(".admin-list small").text()).toContain("BLOCKED");
  });

  it("진행 중 회원 목록 응답은 화면 폐기 후 목적지 URL을 교정하지 않는다", async () => {
    let resolveList!: (value: object) => void;
    route.query = { page: "1", status: "ACTIVE" };
    api.fetchAdminMembers.mockReturnValueOnce(new Promise(resolve => { resolveList = resolve; }));
    const wrapper = mount(AdminMembersView);
    await flushPromises();
    wrapper.unmount();
    route.query = { page: "3", status: "DRAFT" };
    resolveList({ content: [], page: 1, size: 20, totalPages: 0, totalElements: 0 });
    await flushPromises();
    expect(replace).not.toHaveBeenCalled();
    expect(route.query).toEqual({ page: "3", status: "DRAFT" });
  });

  it("결과가 없는 범위 밖 회원 page를 0으로 replace한다", async () => {
    route.query = { page: "3", status: "BLOCKED" };
    api.fetchAdminMembers.mockResolvedValueOnce({ content: [], page: 3, size: 20, totalElements: 0, totalPages: 0 });
    mount(AdminMembersView);
    await flushPromises();
    expect(replace).toHaveBeenCalledWith({ query: { page: "0", status: "BLOCKED" } });
  });

  it("URL의 회원 page와 상태를 조회하고 다음 페이지를 URL에 기록한다", async () => {
    route.query = { page: "1", status: "BLOCKED" };
    api.fetchAdminMembers.mockResolvedValueOnce({ content: [{ id: 1, nickname: "회원", role: "USER", status: "BLOCKED" }], page: 1, size: 20, totalElements: 41, totalPages: 3 });
    const wrapper = mount(AdminMembersView);
    await flushPromises();
    expect(api.fetchAdminMembers).toHaveBeenCalledWith({ status: "BLOCKED", page: 1, size: 20 });
    await wrapper.get("button[data-page='next']").trigger("click");
    expect(push).toHaveBeenCalledWith({ query: { page: "2", status: "BLOCKED" } });
  });

  it("브라우저 이동으로 회원 query가 바뀌면 새 조건을 조회한다", async () => {
    const wrapper = mount(AdminMembersView);
    await flushPromises(); api.fetchAdminMembers.mockClear();
    route.query = { page: "2", status: "WITHDRAWN" };
    await flushPromises();
    expect(api.fetchAdminMembers).toHaveBeenCalledWith({ status: "WITHDRAWN", page: 2, size: 20 });
    wrapper.unmount();
  });

  it("초기 목록 실패 뒤 재시도하면 회원 목록을 복구한다", async () => {
    api.fetchAdminMembers.mockRejectedValueOnce(new Error("temporary"));
    const wrapper = mount(AdminMembersView);
    await flushPromises();
    expect(wrapper.text()).toContain("회원 목록을 불러오지 못했습니다.");
    expect(wrapper.text()).not.toContain("회원을 불러오는 중");
    await wrapper.get("button[data-retry='list']").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("회원");
  });
});
