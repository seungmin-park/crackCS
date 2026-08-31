import { readonly, ref } from "vue";

import {
  fetchCurrentMember,
  login as requestLogin,
  logout as requestLogout,
  type LoginInput,
  type Member,
} from "@/api/auth";
import { ApiClientError, clearCsrfToken } from "@/api/client";

const currentMember = ref<Member | null>(null);
const authenticationResolved = ref(false);
let restoring: Promise<void> | undefined;

export function useAuth() {
  async function restoreAuthentication(): Promise<void> {
    if (authenticationResolved.value) return;
    if (restoring) return restoring;

    restoring = (async () => {
      try {
        currentMember.value = await fetchCurrentMember();
      } catch (error) {
        if (error instanceof ApiClientError && error.status === 401) {
          currentMember.value = null;
        } else {
          throw error;
        }
      } finally {
        authenticationResolved.value = true;
        restoring = undefined;
      }
    })();
    return restoring;
  }

  async function login(input: LoginInput): Promise<Member> {
    const member = await requestLogin(input);
    currentMember.value = member;
    authenticationResolved.value = true;
    return member;
  }

  async function logout(): Promise<void> {
    await requestLogout();
    clearAuthenticationState();
  }

  function clearAuthenticationState(): void {
    currentMember.value = null;
    authenticationResolved.value = true;
    clearCsrfToken();
  }

  return {
    currentMember: readonly(currentMember),
    authenticationResolved: readonly(authenticationResolved),
    restoreAuthentication,
    login,
    logout,
    clearAuthenticationState,
  };
}
