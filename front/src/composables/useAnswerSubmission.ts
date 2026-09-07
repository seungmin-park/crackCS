import { computed, ref } from "vue";

import type { SubmitAnswerRequest } from "@/api/answers";
import { ApiClientError } from "@/api/client";

type Sender<T> = (questionId: number, payload: SubmitAnswerRequest) => Promise<T>;

export function useAnswerSubmission<T>(
  memberId: number | undefined,
  questionId: number,
  send: Sender<T>,
  createRequestId: () => string = () => crypto.randomUUID(),
) {
  const storageKey = memberId === undefined ? undefined : `crackcs:answer-submission:${memberId}:${questionId}`;
  const content = ref("");
  const submitting = ref(false);
  const error = ref<string>();
  const pendingPayload = ref<SubmitAnswerRequest | null>(storageKey ? readPending(storageKey) : null);
  const canSubmit = computed(() => content.value.trim().length > 0 && content.value.length <= 10_000 && !submitting.value && !pendingPayload.value);

  async function sendPayload(payload: SubmitAnswerRequest): Promise<T | undefined> {
    if (submitting.value) return undefined;
    submitting.value = true;
    error.value = undefined;
    pendingPayload.value = payload;
    if (storageKey) writePending(storageKey, payload);
    try {
      const answer = await send(questionId, payload);
      pendingPayload.value = null;
      if (storageKey) removePending(storageKey);
      return answer;
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status >= 400 && caught.status < 500 && caught.status !== 408) {
        pendingPayload.value = null;
        if (storageKey) removePending(storageKey);
        error.value = caught.message;
      } else {
        error.value = "제출 결과를 확인하지 못했어요. 같은 답변으로 다시 확인해 주세요.";
      }
      return undefined;
    } finally {
      submitting.value = false;
    }
  }

  function submit() {
    if (!canSubmit.value) return Promise.resolve(undefined);
    return sendPayload({ requestId: createRequestId(), content: content.value });
  }

  function retry() {
    return pendingPayload.value ? sendPayload(pendingPayload.value) : Promise.resolve(undefined);
  }

  return { content, submitting, error, pendingPayload, canSubmit, submit, retry };
}

export function clearPendingAnswerSubmissions() {
  try {
    const keys = Array.from({ length: sessionStorage.length }, (_, index) => sessionStorage.key(index));
    keys.filter((key): key is string => key?.startsWith("crackcs:answer-submission:") === true).forEach((key) => sessionStorage.removeItem(key));
  } catch { /* browser storage may be unavailable */ }
}

function writePending(storageKey: string, payload: SubmitAnswerRequest) {
  try { sessionStorage.setItem(storageKey, JSON.stringify(payload)); } catch { /* browser storage may be unavailable */ }
}

function removePending(storageKey: string) {
  try { sessionStorage.removeItem(storageKey); } catch { /* browser storage may be unavailable */ }
}

function readPending(storageKey: string): SubmitAnswerRequest | null {
  try {
    const value = sessionStorage.getItem(storageKey);
    if (!value) return null;
    const parsed = JSON.parse(value) as SubmitAnswerRequest;
    return typeof parsed.requestId === "string" && typeof parsed.content === "string" ? parsed : null;
  } catch {
    return null;
  }
}
