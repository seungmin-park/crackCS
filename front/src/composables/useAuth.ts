import { readonly, ref } from "vue";

import {
  fetchCurrentMember,
  login as requestLogin,
  logout as requestLogout,
  type LoginInput,
  type Member,
} from "@/api/auth";
import { ApiClientError, clearCsrfToken } from "@/api/client";
import { clearPendingAnswerSubmissions } from "@/composables/useAnswerSubmission";

const currentMember = ref<Member | null>(null);
const authenticationResolved = ref(false);
let restoring: Promise<void> | undefined;
let authenticationRevision = 0;

export function useAuth() {
  async function restoreAuthentication(): Promise<void> {
    if (authenticationResolved.value) return;
    if (restoring) return restoring;
    const revision = authenticationRevision;

    restoring = (async () => {
      try {
        const member = await fetchCurrentMember();
        if (revision === authenticationRevision) {
          currentMember.value = member;
          authenticationResolved.value = true;
        }
      } catch (error) {
        if (error instanceof ApiClientError && error.status === 401) {
          if (revision === authenticationRevision) {
            currentMember.value = null;
            authenticationResolved.value = true;
          }
        } else {
          throw error;
        }
      } finally {
        restoring = undefined;
      }
    })();
    return restoring;
  }

  async function login(input: LoginInput): Promise<Member> {
    const revision = ++authenticationRevision;
    const credentials = { ...input };
    const member = await requestLogin(credentials);
    if (revision === authenticationRevision) {
      currentMember.value = member;
      authenticationResolved.value = true;
    }
    return member;
  }

  async function logout(): Promise<void> {
    const revision = ++authenticationRevision;
    try {
      await requestLogout();
    } catch (error) {
      if (!(error instanceof ApiClientError && error.status === 401)) throw error;
    }
    if (revision === authenticationRevision) clearAuthenticationStateWithoutInvalidation();
  }

  function clearAuthenticationState(): void {
    authenticationRevision += 1;
    clearAuthenticationStateWithoutInvalidation();
  }

  function clearAuthenticationStateWithoutInvalidation(): void {
    clearPendingAnswerSubmissions();
    currentMember.value = null;
    authenticationResolved.value = true;
    clearCsrfToken();
  }

  function expireSession(): void {
    clearAuthenticationState();
  }

  function captureSessionExpiration(): () => boolean {
    const revision = authenticationRevision;
    return () => {
      if (revision !== authenticationRevision) return false;
      expireSession();
      return true;
    };
  }

  return {
    currentMember: readonly(currentMember),
    authenticationResolved: readonly(authenticationResolved),
    restoreAuthentication,
    login,
    logout,
    clearAuthenticationState,
    expireSession,
    captureSessionExpiration,
  };
}
