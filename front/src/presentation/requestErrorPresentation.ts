import { ApiClientError } from "@/api/client";

export type RequestErrorPresentation = {
  kind: "network" | "temporary" | "request" | "validation" | "unauthenticated" | "forbidden" | "not-found" | "conflict" | "timeout" | "rate-limited";
  message: string;
  retryable: boolean;
  fieldErrors: Record<string, string>;
};

export function presentRequestError(error: unknown): RequestErrorPresentation {
  if (!(error instanceof ApiClientError)) {
    return { kind: "network", message: "연결을 확인한 뒤 다시 시도해 주세요.", retryable: true, fieldErrors: {} };
  }
  if (error.status >= 500) {
    return { kind: "temporary", message: "서버가 요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.", retryable: true, fieldErrors: {} };
  }
  if (error.status === 408) {
    return { kind: "timeout", message: "응답이 늦어지고 있어요. 잠시 후 다시 확인해 주세요.", retryable: true, fieldErrors: {} };
  }
  if (error.status === 429) {
    return { kind: "rate-limited", message: "요청이 너무 많아요. 잠시 기다린 뒤 다시 시도해 주세요.", retryable: true, fieldErrors: {} };
  }
  const kinds: Record<number, RequestErrorPresentation["kind"]> = {
    400: "validation", 401: "unauthenticated", 403: "forbidden", 404: "not-found", 409: "conflict",
  };
  return {
    kind: kinds[error.status] ?? "request", message: error.message, retryable: false,
    fieldErrors: error.status === 400 ? Object.fromEntries(error.fieldErrors.map(({ field, reason }) => [field, reason])) : {},
  };
}
