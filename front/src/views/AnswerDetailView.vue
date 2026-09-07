<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { fetchAnswer, fetchAnswerEvaluation, type AnswerResponse } from "@/api/answers";
import { ApiClientError } from "@/api/client";
import EvaluationPanel from "@/components/EvaluationPanel.vue";
import QuestionState from "@/components/QuestionState.vue";

const route = useRoute();
const answer = ref<AnswerResponse>();
const error = ref(false);
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
      if (evaluation.status === "EVALUATING") schedulePoll(answerId, activeGeneration);
    } catch (failure) {
      if (disposed || activeGeneration !== generation) return;
      consecutivePollFailures++;
      const retryable = !(failure instanceof ApiClientError) || failure.status >= 500;
      if (retryable && consecutivePollFailures < MAX_POLL_FAILURES) {
        schedulePoll(answerId, activeGeneration);
      } else {
        error.value = true;
      }
    }
  }, 2000);
}

async function loadAnswer(answerId = String(route.params.answerId)) {
  cancelTimer();
  const activeGeneration = ++generation;
  answer.value = undefined;
  error.value = false;
  consecutivePollFailures = 0;
  try {
    const loaded = await fetchAnswer(answerId);
    if (disposed || activeGeneration !== generation) return;
    answer.value = loaded;
    if (loaded.evaluation.status === "EVALUATING") schedulePoll(answerId, activeGeneration);
  } catch {
    if (!disposed && activeGeneration === generation) error.value = true;
  }
}

watch(() => String(route.params.answerId), (answerId) => loadAnswer(answerId), { immediate: true });
onBeforeUnmount(() => { disposed = true; generation++; cancelTimer(); });
</script>

<template>
  <main class="page-shell answer-detail-shell">
    <RouterLink class="back-link" to="/answers">← 답변 이력</RouterLink>
    <QuestionState v-if="error" title="답변을 불러오지 못했어요" description="잠시 후 다시 시도해 주세요." action-label="다시 불러오기" @action="loadAnswer" />
    <article v-else-if="answer" class="answer-detail-grid">
      <section class="answer-copy"><p class="eyebrow">질문</p><h1>{{ answer.questionContent }}</h1><p class="submitted-at">{{ new Date(answer.submittedAt).toLocaleString('ko-KR') }}</p><h2>내 답변</h2><p class="answer-content">{{ answer.content }}</p></section>
      <EvaluationPanel :evaluation="answer.evaluation" />
    </article>
  </main>
</template>
