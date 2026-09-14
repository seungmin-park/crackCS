import { beforeEach, describe, expect, it, vi } from "vitest";

const { setSessionExpiredHandler, captureSessionExpiration, expireIfCurrent, replace } = vi.hoisted(() => ({
  setSessionExpiredHandler: vi.fn(),
  captureSessionExpiration: vi.fn(),
  expireIfCurrent: vi.fn(),
  replace: vi.fn(),
}));

vi.mock("@/api/client", () => ({ setSessionExpiredHandler }));
vi.mock("@/composables/useAuth", () => ({ useAuth: () => ({ captureSessionExpiration }) }));

import { connectAuthenticationLifecycle } from "@/authLifecycle";

describe("애플리케이션 인증 만료 조립", () => {
  beforeEach(() => {
    setSessionExpiredHandler.mockReset();
    captureSessionExpiration.mockReset();
    expireIfCurrent.mockReset();
    captureSessionExpiration.mockReturnValue(expireIfCurrent);
    replace.mockReset();
  });

  it("보호 요청 만료 시 인증을 정리하고 현재 내부 경로를 로그인 redirect로 보존한다", async () => {
    connectAuthenticationLifecycle({ currentRoute: { value: { fullPath: "/questions/7" } }, replace } as never);
    const capture = setSessionExpiredHandler.mock.calls[0]![0] as () => () => Promise<void>;
    const handler = capture();
    expireIfCurrent.mockReturnValue(true);

    await handler();

    expect(expireIfCurrent).toHaveBeenCalledOnce();
    expect(replace).toHaveBeenCalledWith({ name: "login", query: { redirect: "/questions/7" } });
  });

  it("로그인 화면에서 받은 만료 알림은 중복 이동하지 않는다", async () => {
    connectAuthenticationLifecycle({ currentRoute: { value: { fullPath: "/login", name: "login" } }, replace } as never);
    const capture = setSessionExpiredHandler.mock.calls[0]![0] as () => () => Promise<void>;
    const handler = capture();
    expireIfCurrent.mockReturnValue(true);

    await handler();

    expect(expireIfCurrent).toHaveBeenCalledOnce();
    expect(replace).not.toHaveBeenCalled();
  });

  it("요청 뒤 인증 세대가 바뀌면 만료 알림으로 상태나 경로를 바꾸지 않는다", async () => {
    connectAuthenticationLifecycle({ currentRoute: { value: { fullPath: "/questions" } }, replace } as never);
    const capture = setSessionExpiredHandler.mock.calls[0]![0] as () => () => Promise<void>;
    const handler = capture();
    expireIfCurrent.mockReturnValue(false);

    await handler();

    expect(replace).not.toHaveBeenCalled();
  });
});
