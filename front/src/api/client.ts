export class ApiClientError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fieldErrors: FieldError[] = [],
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
  message?: string;
  fieldErrors?: FieldError[];
};

export async function get<T>(path: string): Promise<T> {
  return request<T>(path, { method: "GET" });
}

export async function post<T>(path: string, body?: unknown): Promise<T> {
  return write<T>(path, "POST", body);
}

export async function patch<T>(path: string, body: unknown): Promise<T> {
  return write<T>(path, "PATCH", body);
}

export async function put<T>(path: string, body: unknown): Promise<T> {
  return write<T>(path, "PUT", body);
}

async function write<T>(path: string, method: "POST" | "PATCH" | "PUT", body?: unknown): Promise<T> {
  const csrf = await fetchCsrfToken();
  return request<T>(path, {
    method,
    headers: { [csrf.headerName]: csrf.token },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
}

export function clearCsrfToken(): void {
  cachedCsrfToken = undefined;
}

type CsrfToken = {
  token: string;
  headerName: string;
};

let cachedCsrfToken: CsrfToken | undefined;

async function fetchCsrfToken(): Promise<CsrfToken> {
  if (!cachedCsrfToken) {
    cachedCsrfToken = await request<CsrfToken>("/api/auth/csrf", { method: "GET" });
  }
  return cachedCsrfToken;
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
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
    throw new ApiClientError(
      response.status,
      body.message ?? "요청을 처리하지 못했습니다.",
      body.fieldErrors ?? [],
    );
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
