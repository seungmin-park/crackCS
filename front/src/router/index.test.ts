import { flushPromises, mount } from "@vue/test-utils";
import { defineComponent, ref } from "vue";
import { beforeEach, describe, expect, it, vi } from "vitest";

const currentMember = ref<{ role: "USER" | "ADMIN" } | null>(null);
const authenticationResolved = ref(true);
const restoreAuthentication = vi.fn();
const { fetchMyAnswers, fetchAnswer } = vi.hoisted(() => ({
  fetchMyAnswers: vi.fn(),
  fetchAnswer: vi.fn(),
}));

vi.mock("@/composables/useAuth", () => ({
  useAuth: () => ({ currentMember, authenticationResolved, restoreAuthentication }),
}));
vi.mock("@/api/answers", async (importOriginal) => ({
  ...await importOriginal<typeof import("@/api/answers")>(),
  fetchMyAnswers,
  fetchAnswer,
}));

import router, { authorizationGuard } from "@/router";

describe("인증 라우트 가드", () => {
  it("실제 router도 인증 장애 시 복구 화면을 열어 원래 경로를 유지한다", async () => {
    restoreAuthentication.mockRejectedValue(new TypeError("network"));
    await router.push("/questions/22");
    expect(router.currentRoute.value.name).toBe("connection-error");
    expect(router.currentRoute.value.query.redirect).toBe("/questions/22");
  });

  it("인증 복원이 연결 오류로 실패하면 빈 화면 대신 원래 주소를 보관한 복구 화면으로 이동한다", async () => {
    restoreAuthentication.mockRejectedValueOnce(new TypeError("network"));
    await expect(authorizationGuard({ meta: { requiresAuth: true }, fullPath: "/answers/31" } as never))
      .resolves.toEqual({ name: "connection-error", query: { redirect: "/answers/31" } });
  });

  it("연결 오류 화면 자체는 인증 조회를 반복하지 않는다", async () => {
    await expect(authorizationGuard({ name: "connection-error", meta: {}, fullPath: "/connection-error" } as never))
      .resolves.toBe(true);
    expect(restoreAuthentication).not.toHaveBeenCalled();
  });

  it("학습 홈과 지식 지도는 로그인한 학습자 화면이다", () => {
    expect(router.resolve("/").name).toBe("learning-home");
    expect(router.resolve("/").meta).toMatchObject({ requiresAuth: true, requiresUser: true });
    expect(router.resolve("/knowledge-map").meta).toMatchObject({ requiresAuth: true, requiresUser: true });
  });

  it("답변 이력과 상세 route는 실제 metadata로 USER 전용 경계를 선언한다", () => {
    expect(router.resolve("/answers").meta).toMatchObject({ requiresAuth: true, requiresUser: true });
    expect(router.resolve("/answers/31").meta).toMatchObject({ requiresAuth: true, requiresUser: true });
  });

  it.each(["/answers", "/answers/31"])("ADMIN이 %s API 화면을 mount하기 전에 관리자 홈으로 이동한다", async (path) => {
    currentMember.value = { role: "ADMIN" };
    const wrapper = mount(defineComponent({ template: "<RouterView />" }), {
      global: { plugins: [router] },
    });

    await router.push(path);
    await flushPromises();

    expect(router.currentRoute.value.name).toBe("admin");
    expect(fetchMyAnswers).not.toHaveBeenCalled();
    expect(fetchAnswer).not.toHaveBeenCalled();
    wrapper.unmount();
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
    fetchMyAnswers.mockReset();
    fetchAnswer.mockReset();
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
