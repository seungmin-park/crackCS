import { ref } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";

const currentMember = ref<{ role: "USER" | "ADMIN" } | null>(null);
const restoreAuthentication = vi.fn();

vi.mock("@/composables/useAuth", () => ({
  useAuth: () => ({ currentMember, restoreAuthentication }),
}));

import { authorizationGuard } from "@/router";

describe("인증 라우트 가드", () => {
  beforeEach(() => {
    currentMember.value = null;
    restoreAuthentication.mockReset();
    restoreAuthentication.mockResolvedValue(undefined);
  });

  it("비로그인 사용자가 보호 화면에 접근하면 로그인 화면으로 안내한다", async () => {
    const result = await authorizationGuard({
      meta: { requiresAuth: true },
      fullPath: "/questions/7",
    } as never);

    expect(result).toEqual({ name: "login", query: { redirect: "/questions/7" } });
  });

  it("USER가 관리자 화면에 접근하면 접근 거부 화면으로 안내한다", async () => {
    currentMember.value = { role: "USER" };

    const result = await authorizationGuard({
      meta: { requiresAuth: true, requiresAdmin: true },
      fullPath: "/admin",
    } as never);

    expect(result).toEqual({ name: "admin-forbidden" });
  });

  it("ADMIN은 관리자 화면에 접근할 수 있다", async () => {
    currentMember.value = { role: "ADMIN" };

    const result = await authorizationGuard({
      meta: { requiresAuth: true, requiresAdmin: true },
      fullPath: "/admin",
    } as never);

    expect(result).toBe(true);
  });
});
