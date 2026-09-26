export class ApiClientError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fieldErrors: FieldError[] = [],
    public readonly code?: string,
    public readonly requestId?: string,
  ) {
    super(message);
    this.name = "ApiClientError";
  }
}

export type FieldError = {
  field: string;
  reason: string;
};

type ApiErrorBody = {
  code?: string;
  message?: string;
  fieldErrors?: FieldError[];
  requestId?: string;
};

export type RequestAuthentication = "required" | "credentials" | "anonymous";

export type RequestPolicy = {
  authentication?: RequestAuthentication;
};

export async function get<T>(path: string, policy?: RequestPolicy): Promise<T> {
  const expirationGuard = captureExpirationGuard(policy?.authentication ?? "required");
  return request<T>(path, { method: "GET" }, policy, expirationGuard);
}

export async function post<T>(path: string, body?: unknown, headers?: Record<string, string>, policy?: RequestPolicy): Promise<T> {
  return write<T>(path, "POST", body, headers, policy);
}

export async function patch<T>(path: string, body: unknown): Promise<T> {
  return write<T>(path, "PATCH", body);
}

export async function put<T>(path: string, body: unknown): Promise<T> {
  return write<T>(path, "PUT", body);
}

async function write<T>(path: string, method: "POST" | "PATCH" | "PUT", body?: unknown, headers?: Record<string, string>, policy?: RequestPolicy): Promise<T> {
  const expirationGuard = captureExpirationGuard(policy?.authentication ?? "required");
  const csrf = await getCsrfToken(() => request<CsrfToken>("/api/auth/csrf", { method: "GET" }, { authentication: "anonymous" }));
  return request<T>(path, {
    method,
    headers: { ...headers, [csrf.headerName]: csrf.token },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  }, policy, expirationGuard);
}

async function request<T>(path: string, init: RequestInit, policy: RequestPolicy = {}, expirationGuard?: SessionExpiredGuard): Promise<T> {
  const response = await fetch(path, {
    ...init,
    credentials: "same-origin",
    headers: {
      Accept: "application/json",
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...init.headers,
    },
  });

  if (!response.ok) {
    const body = await parseError(response);
    const error = new ApiClientError(
      response.status,
      body.message ?? "요청을 처리하지 못했습니다.",
      body.fieldErrors ?? [],
      body.code,
      body.requestId,
    );
    if (response.status === 401 && (policy.authentication ?? "required") === "required") {
      await notifySessionExpired(expirationGuard);
    }
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

async function parseError(response: Response): Promise<ApiErrorBody> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return {};
  }
}
import { clearCsrfToken, getCsrfToken, type CsrfToken } from "@/api/csrfTokenStore";
import { captureExpirationGuard, notifySessionExpired, setSessionExpiredHandler, type SessionExpiredGuard } from "@/api/sessionExpiration";

export { clearCsrfToken, setSessionExpiredHandler };
