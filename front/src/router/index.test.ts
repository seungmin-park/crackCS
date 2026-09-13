import { ref } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";

const currentMember = ref<{ role: "USER" | "ADMIN" } | null>(null);
const restoreAuthentication = vi.fn();

vi.mock("@/composables/useAuth", () => ({
  useAuth: () => ({ currentMember, restoreAuthentication }),
}));

import router, { authorizationGuard } from "@/router";

describe("인증 라우트 가드", () => {
  it("학습 홈과 지식 지도는 로그인한 학습자 화면이다", () => {
    expect(router.resolve("/").name).toBe("learning-home");
    expect(router.resolve("/").meta).toMatchObject({ requiresAuth: true, requiresUser: true });
    expect(router.resolve("/knowledge-map").meta).toMatchObject({ requiresAuth: true, requiresUser: true });
  });

  it("ADMIN이 학습자 전용 화면에 접근하면 관리자 홈으로 이동한다", async () => {
    currentMember.value = { role: "ADMIN" };
    expect(await authorizationGuard({ meta: { requiresAuth: true, requiresUser: true }, fullPath: "/" } as never))
      .toEqual({ name: "admin" });
  });
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
