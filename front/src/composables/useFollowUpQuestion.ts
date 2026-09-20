import { onBeforeUnmount, ref, watch, type Ref } from "vue";

import {
  fetchFollowUpQuestion,
  type FollowUpQuestionResponse,
} from "@/api/answers";
import { ApiClientError } from "@/api/client";

type FollowUpQuestionErrorKind = "forbidden" | "not-found" | "permanent" | "temporary" | "timeout";
type FollowUpQuestionError = { kind: FollowUpQuestionErrorKind; retryable: boolean };
type FollowUpQuestionRequest = (answerId: number | string) => Promise<FollowUpQuestionResponse>;

const POLL_INTERVAL_MS = 2_000;
const MAX_POLL_REQUESTS = 15;
const MAX_CONSECUTIVE_FAILURES = 3;

export function useFollowUpQuestion(
  answerId: Ref<number | string>,
  request: FollowUpQuestionRequest = fetchFollowUpQuestion,
) {
  const result = ref<FollowUpQuestionResponse>();
  const loading = ref(true);
  const error = ref<FollowUpQuestionError>();
  let timer: ReturnType<typeof setTimeout> | undefined;
  let generation = 0;
  let disposed = false;
  let requestCount = 0;
  let consecutiveFailures = 0;

  function cancelTimer() {
    if (timer) clearTimeout(timer);
    timer = undefined;
  }

  function schedulePoll(activeAnswerId: number | string, activeGeneration: number) {
    if (disposed || activeGeneration !== generation) return;
    timer = setTimeout(() => void load(activeAnswerId, activeGeneration), POLL_INTERVAL_MS);
  }

  async function load(activeAnswerId: number | string, activeGeneration: number) {
    requestCount += 1;
    try {
      const loaded = await request(activeAnswerId);
      if (disposed || activeGeneration !== generation) return;

      result.value = loaded;
      loading.value = false;
      error.value = undefined;
      consecutiveFailures = 0;

      if (loaded.status === "PENDING" || loaded.status === "PROCESSING") {
        if (requestCount < MAX_POLL_REQUESTS) schedulePoll(activeAnswerId, activeGeneration);
        else error.value = { kind: "timeout", retryable: true };
      }
    } catch (caught) {
      if (disposed || activeGeneration !== generation) return;

      const terminal = terminalError(caught);
      if (terminal) {
        loading.value = false;
        error.value = terminal;
        return;
      }

      consecutiveFailures += 1;
      if (requestCount >= MAX_POLL_REQUESTS) error.value = { kind: "timeout", retryable: true };
      else if (consecutiveFailures < MAX_CONSECUTIVE_FAILURES) schedulePoll(activeAnswerId, activeGeneration);
      else error.value = { kind: "temporary", retryable: true };
      loading.value = !result.value && !error.value;
    }
  }

  function begin(activeAnswerId: number | string) {
    cancelTimer();
    const activeGeneration = ++generation;
    result.value = undefined;
    loading.value = true;
    error.value = undefined;
    requestCount = 0;
    consecutiveFailures = 0;
    void load(activeAnswerId, activeGeneration);
  }

  async function retry() {
    cancelTimer();
    const activeGeneration = ++generation;
    result.value = undefined;
    loading.value = true;
    error.value = undefined;
    requestCount = 0;
    consecutiveFailures = 0;
    await load(answerId.value, activeGeneration);
  }

  watch(answerId, begin, { immediate: true });
  onBeforeUnmount(() => {
    disposed = true;
    generation += 1;
    cancelTimer();
  });

  return { result, loading, error, retry };
}

function terminalError(caught: unknown): FollowUpQuestionError | undefined {
  if (!(caught instanceof ApiClientError) || caught.status === 408 || caught.status >= 500) return undefined;
  if (caught.status === 404) return { kind: "not-found", retryable: false };
  if (caught.status === 401 || caught.status === 403) return { kind: "forbidden", retryable: false };
  return { kind: "permanent", retryable: false };
}
