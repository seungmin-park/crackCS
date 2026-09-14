import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

const api = vi.hoisted(() => ({ fetchAdminMembers: vi.fn(), updateMemberStatus: vi.fn() }));
vi.mock("@/api/admin/members", async importOriginal => ({ ...(await importOriginal<typeof import("@/api/admin/members")>()), ...api }));
import AdminMembersView from "@/views/admin/AdminMembersView.vue";

describe("관리자 회원 화면", () => {
  beforeEach(() => {
    Object.values(api).forEach(mock => mock.mockReset());
    api.fetchAdminMembers.mockResolvedValue({ content: [{ id: 1, nickname: "회원", role: "USER", status: "ACTIVE" }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
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
