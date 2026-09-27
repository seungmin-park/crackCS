<script setup lang="ts">
import RequestFailure from "@/components/RequestFailure.vue";
import { presentRequestError, type RequestErrorPresentation } from "@/presentation/requestErrorPresentation";

import { computed, onBeforeUnmount, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchMyAnswers, type AnswerResponse } from "@/api/answers";
import QuestionState from "@/components/QuestionState.vue";
import { presentEvaluation } from "@/presentation/evaluationPresentation";

const route = useRoute();
const router = useRouter();
const answers = ref<AnswerResponse[]>([]);
const totalPages = ref(0);
const loading = ref(true);
const error = ref<RequestErrorPresentation>();
const page = computed(() => {
  const value = Number(route.query.page ?? 0);
  return Number.isSafeInteger(value) && value >= 0 ? value : 0;
});
let generation = 0;
let disposed = false;
function evaluationLabel(answer: AnswerResponse) {
  return presentEvaluation(answer.evaluation.status, answer.evaluation.verdict).label;
}
async function loadAnswers() {
  const activeGeneration = ++generation;
  const requestedPage = page.value;
  loading.value = true;
  error.value = undefined;
  try {
    const answerPage = await fetchMyAnswers({ page: requestedPage, size: 20 });
    if (disposed || activeGeneration !== generation) return;
    const lastPage = Math.max(answerPage.totalPages - 1, 0);
    if (requestedPage > lastPage) {
      void router.replace({ query: { ...route.query, page: String(lastPage) } });
      return;
    }
    answers.value = answerPage.content;
    totalPages.value = answerPage.totalPages;
  } catch (caught) {
    if (!disposed && activeGeneration === generation) error.value = presentRequestError(caught);
  } finally {
    if (!disposed && activeGeneration === generation) loading.value = false;
  }
}
function goToPage(next: number) {
  void router.push({ query: { ...route.query, page: String(next) } });
}
watch(page, loadAnswers, { immediate: true });
onBeforeUnmount(() => {
  disposed = true;
  generation++;
});
</script>

<template>
  <main class="page-shell history-shell">
    <header class="page-intro">
      <p class="eyebrow">학습 기록</p>
      <h1>내 답변 이력</h1>
      <p>제출한 설명과 평가 결과를 다시 확인하세요.</p>
    </header>
    <p v-if="loading" role="status" aria-busy="true" class="admin-loading">답변 이력을 불러오는 중…</p>
    <RequestFailure v-else-if="error" :failure="error" title="답변 이력을 불러오지 못했어요" @retry="loadAnswers" />
    <QuestionState
      v-else-if="!answers.length"
      kind="empty"
      title="아직 제출한 답변이 없어요"
      description="문제 하나를 골라 내 언어로 설명해 보세요."
      action-label="문제 보러 가기"
      @action="$router.push('/questions')"
    />
    <ol v-else class="answer-history-list">
      <li v-for="answer in answers" :key="answer.answerId">
        <RouterLink :to="{ name: 'answer-detail', params: { answerId: answer.answerId } }">
          <div>
            <span>{{ new Date(answer.submittedAt).toLocaleDateString('ko-KR') }}</span>
            <strong>{{ answer.questionContent }}</strong>
            <p>{{ answer.content }}</p>
          </div>
          <span class="evaluation-badge" :data-status="answer.evaluation.status">
            {{ evaluationLabel(answer) }}
          </span>
        </RouterLink>
      </li>
    </ol>
    <nav
      v-if="!loading && !error && (totalPages > 1 || page > 0)"
      class="pagination"
      aria-label="답변 이력 페이지"
    >
      <button type="button" aria-label="이전 페이지" :disabled="page === 0" @click="goToPage(page - 1)">← 이전</button>
      <span>{{ page + 1 }} / {{ Math.max(1, totalPages) }}</span>
      <button type="button" aria-label="다음 페이지" :disabled="page + 1 >= totalPages" @click="goToPage(page + 1)">다음 →</button>
    </nav>
  </main>
</template>
