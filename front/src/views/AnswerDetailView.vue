<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { fetchAnswer, fetchAnswerEvaluation, type AnswerResponse } from "@/api/answers";
import RequestFailure from "@/components/RequestFailure.vue";
import { presentRequestError, type RequestErrorPresentation } from "@/presentation/requestErrorPresentation";

import EvaluationPanel from "@/components/EvaluationPanel.vue";
import FollowUpQuestionPanel from "@/components/FollowUpQuestionPanel.vue";

const route = useRoute();
const answer = ref<AnswerResponse>();
const error = ref<RequestErrorPresentation>();
const loading = ref(true);
const evaluationComplete = computed(() => answer.value !== undefined
  && ["EVALUATED", "NEEDS_REVIEW", "FAILED"].includes(answer.value.evaluation.status));
let timer: ReturnType<typeof setTimeout> | undefined;
let generation = 0;
let disposed = false;
let consecutivePollFailures = 0;
const MAX_POLL_FAILURES = 3;

function cancelTimer() {
  if (timer) clearTimeout(timer);
  timer = undefined;
}

function schedulePoll(answerId: string, activeGeneration: number) {
  if (disposed || activeGeneration !== generation) return;
  timer = setTimeout(async () => {
    if (disposed || activeGeneration !== generation) return;
    try {
      const evaluation = await fetchAnswerEvaluation(answerId);
      if (disposed || activeGeneration !== generation || !answer.value) return;
      answer.value.evaluation = evaluation;
      consecutivePollFailures = 0;
      if (evaluation.status === "EVALUATING" || evaluation.status === "PROCESSING") schedulePoll(answerId, activeGeneration);
    } catch (failure) {
      if (disposed || activeGeneration !== generation) return;
      consecutivePollFailures++;
      const presentation = presentRequestError(failure);
      const retryable = presentation.retryable && presentation.kind !== "rate-limited";
      if (retryable && consecutivePollFailures < MAX_POLL_FAILURES) {
        schedulePoll(answerId, activeGeneration);
      } else {
        if (!presentation.retryable) answer.value = undefined;
        error.value = presentation;
      }
    }
  }, 2000);
}

async function loadAnswer(answerId = String(route.params.answerId)) {
  cancelTimer();
  const activeGeneration = ++generation;
  if (String(answer.value?.answerId) !== answerId) answer.value = undefined;
  loading.value = true;
  error.value = undefined;
  consecutivePollFailures = 0;
  try {
    const loaded = await fetchAnswer(answerId);
    if (disposed || activeGeneration !== generation) return;
    answer.value = loaded;
    if (loaded.evaluation.status === "EVALUATING" || loaded.evaluation.status === "PROCESSING") schedulePoll(answerId, activeGeneration);
  } catch (failure) {
    if (disposed || activeGeneration !== generation) return;
    const presentation = presentRequestError(failure);
    if (!presentation.retryable) answer.value = undefined;
    error.value = presentation;
  } finally {
    if (!disposed && activeGeneration === generation) loading.value = false;
  }
}

watch(() => String(route.params.answerId), (answerId) => loadAnswer(answerId), { immediate: true });
onBeforeUnmount(() => { disposed = true; generation++; cancelTimer(); });
</script>

<template>
  <main class="page-shell answer-detail-shell">
    <RouterLink class="back-link" to="/answers">← 답변 이력</RouterLink>
    <RequestFailure v-if="error" :failure="error" title="답변을 불러오지 못했어요" @retry="loadAnswer()" />
    <p v-if="loading" role="status" aria-busy="true">답변을 불러오는 중…</p>
    <template v-if="answer">
      <article class="answer-detail-grid">
        <section class="answer-copy"><p class="eyebrow">질문</p><h1>{{ answer.questionContent }}</h1><p class="submitted-at">{{ new Date(answer.submittedAt).toLocaleString('ko-KR') }}</p><h2>내 답변</h2><p class="answer-content">{{ answer.content }}</p></section>
        <EvaluationPanel :evaluation="answer.evaluation" />
      </article>
      <FollowUpQuestionPanel v-if="evaluationComplete" :answer-id="answer.answerId" />
    </template>
  </main>
</template>
