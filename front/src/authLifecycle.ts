import type { Router } from "vue-router";

import { setSessionExpiredHandler } from "@/api/client";
import { useAuth } from "@/composables/useAuth";

export function connectAuthenticationLifecycle(router: Router): void {
  const { captureSessionExpiration } = useAuth();
  setSessionExpiredHandler(() => {
    const route = router.currentRoute.value;
    const redirect = route.fullPath.startsWith("/") && !route.fullPath.startsWith("//")
      ? route.fullPath
      : "/";
    const expireIfCurrent = captureSessionExpiration();
    return async () => {
      if (!expireIfCurrent() || route.name === "login") return;
      await router.replace({ name: "login", query: { redirect } });
    };
  });
}
