import { flushPromises, mount } from "@vue/test-utils";
import { computed, ref } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";

const member = ref<{ role: "USER" | "ADMIN" } | null>(null);
const resolved = ref(false);

vi.mock("@/composables/useAuth", () => ({
  useAuth: () => ({ currentMember: computed(() => member.value), authenticationResolved: computed(() => resolved.value) }),
}));

import AdminLayoutView from "@/views/admin/AdminLayoutView.vue";

function mountView() {
  return mount(AdminLayoutView, {
    global: {
      stubs: {
        RouterLink: { props: ["to"], template: "<a :href='to'><slot /></a>" },
        RouterView: { template: "<div>관리 화면</div>" },
      },
    },
  });
}

describe("관리자 레이아웃", () => {
  beforeEach(() => { member.value = null; resolved.value = false; });

  it("ADMIN 여부를 확인하는 동안 로딩 화면을 표시한다", () => {
    expect(mountView().text()).toContain("관리자 권한을 확인하고 있어요");
  });

  it("인증된 USER에게 접근 거부 화면을 표시한다", async () => {
    const wrapper = mountView();
    member.value = { role: "USER" }; resolved.value = true;
    await flushPromises();
    expect(wrapper.text()).toContain("관리자 권한이 필요합니다");
  });

  it("ADMIN에게 콘텐츠 관리 메뉴와 하위 화면을 표시한다", async () => {
    member.value = { role: "ADMIN" }; resolved.value = true;
    const wrapper = mountView();
    await flushPromises();
    expect(wrapper.text()).toContain("분류와 개념");
    expect(wrapper.text()).toContain("근거 문서");
    expect(wrapper.text()).toContain("문제");
    expect(wrapper.text()).toContain("관리 화면");
  });
});
