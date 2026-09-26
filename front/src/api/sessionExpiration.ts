export type SessionExpiredGuard = () => void | Promise<void>;
type SessionExpiredHandler = () => SessionExpiredGuard;

let sessionExpiredHandler: SessionExpiredHandler | undefined;

export function setSessionExpiredHandler(handler: SessionExpiredHandler | undefined): void {
  sessionExpiredHandler = handler;
}

export function captureExpirationGuard(authentication: "required" | "credentials" | "anonymous"): SessionExpiredGuard | undefined {
  return authentication === "required" ? sessionExpiredHandler?.() : undefined;
}

export async function notifySessionExpired(expirationGuard?: SessionExpiredGuard): Promise<void> {
  try {
    await expirationGuard?.();
  } catch {
    // HTTP errors take precedence over session cleanup and navigation failures.
  }
}
