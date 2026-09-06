<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { difficultyLabel, fetchQuestions, type PublicQuestionPage, type QuestionDifficulty } from "@/api/questions";
import QuestionState from "@/components/QuestionState.vue";

const result = ref<PublicQuestionPage>();
const route = useRoute();
const router = useRouter();
const page = computed(() => {
  const value = Number(route.query.page ?? 1);
  return Number.isSafeInteger(value) && value > 0 && value <= 2147483647 ? value - 1 : 0;
});
const difficulty = computed<QuestionDifficulty | undefined>(() => {
  const value = route.query.difficulty;
  return value === "BASIC" || value === "INTERMEDIATE" || value === "ADVANCED" ? value : undefined;
});
const loading = ref(true);
const failed = ref(false);
let requestId = 0;
const filters: { value: QuestionDifficulty | undefined; title: string; description: string }[] = [
  { value: undefined, title: "전체 문제", description: "모든 난이도 둘러보기" },
  { value: "BASIC", title: "기본", description: "개념부터 차근차근" },
  { value: "INTERMEDIATE", title: "중급", description: "차이와 원리 설명하기" },
  { value: "ADVANCED", title: "심화", description: "상황에 적용해 보기" },
];

async function loadQuestions() {
  const currentRequest = ++requestId;
  loading.value = true;
  failed.value = false;
  try {
    const response = await fetchQuestions({ page: page.value, difficulty: difficulty.value });
    if (currentRequest === requestId) result.value = response;
  } catch {
    if (currentRequest === requestId) failed.value = true;
  } finally {
    if (currentRequest === requestId) loading.value = false;
  }
}
function filterBy(value: QuestionDifficulty | undefined) {
  void router.push({ query: { ...route.query, page: "1", difficulty: value } });
}
function goToPage(value: number) {
  void router.push({ query: { ...route.query, page: String(value + 1) } });
}
watch([page, difficulty], loadQuestions, { immediate: true });
</script>

<template>
  <main class="library-layout">
    <aside class="library-sidebar" aria-label="문제 난이도">
      <p class="sidebar-heading">문제집</p>
      <div class="library-filters">
        <button v-for="filter in filters" :key="filter.value ?? 'all'" type="button"
          :data-filter="filter.value ?? 'all'" :aria-pressed="difficulty === filter.value"
          @click="filterBy(filter.value)">
          <span>{{ filter.title }}</span><small>{{ filter.description }}</small>
        </button>
      </div>
      <div class="sidebar-note"><span class="eyebrow">작은 학습 습관</span><p>읽고 넘어가기 전에,<br />내 말로 설명해 보세요.</p></div>
    </aside>

    <section class="library-content">
      <header class="page-intro">
        <p class="eyebrow">컴퓨터 과학 · 백엔드</p>
        <h1>어떤 개념부터<br class="mobile-break" /> 풀어볼까요?</h1>
        <p>질문을 고르고, 아는 만큼 설명해 보세요.</p>
      </header>
      <div class="section-heading library-toolbar">
        <h2>{{ difficulty ? difficultyLabel(difficulty) + ' 문제' : '전체 문제' }}</h2>
        <span v-if="!loading && !failed && result" role="status">전체 {{ result.totalElements }}문제</span>
      </div>
      <section v-if="loading" class="question-skeletons" aria-label="문제 목록을 불러오는 중" aria-busy="true">
        <div v-for="index in 5" :key="index" class="skeleton-line" />
      </section>
      <QuestionState v-else-if="failed" title="문제를 불러오지 못했어요"
        description="서버 연결을 확인한 뒤 다시 시도해 주세요." action-label="다시 불러오기" @action="loadQuestions" />
      <QuestionState v-else-if="!result?.content.length"
        :title="difficulty ? '이 난이도에는 아직 문제가 없어요' : '아직 공개된 문제가 없어요'"
        :description="difficulty ? '다른 난이도를 선택해 문제를 둘러보세요.' : '새로운 문제가 준비되면 이곳에 표시됩니다.'" />
      <template v-else>
        <div class="question-table-heading" aria-hidden="true"><span>번호</span><span>질문</span><span>주제</span><span>난이도</span><span /></div>
        <ol class="question-list">
          <li v-for="(question, index) in result.content" :key="question.id">
            <RouterLink class="question-row" :to="{ path: `/questions/${question.id}`, query: route.query }">
              <span class="question-number">{{ String(result.page * result.size + index + 1).padStart(2, '0') }}</span>
              <h3>{{ question.content }}</h3>
              <span class="question-topic">{{ question.topic.name }}</span>
              <span class="difficulty-pill" :data-difficulty="question.difficulty">{{ difficultyLabel(question.difficulty) }}</span>
              <span class="question-arrow" aria-hidden="true">↗</span>
            </RouterLink>
          </li>
        </ol>
      </template>
      <nav v-if="!failed && result && result.totalPages > 1" class="pagination" aria-label="문제 목록 페이지">
        <button type="button" aria-label="이전 페이지" :disabled="loading || page === 0" @click="goToPage(page - 1)">← 이전</button>
        <span aria-live="polite">{{ page + 1 }} / {{ result.totalPages }}</span>
        <button type="button" aria-label="다음 페이지" :disabled="loading || page + 1 >= result.totalPages" @click="goToPage(page + 1)">다음 →</button>
      </nav>
      <div class="library-footnote"><span aria-hidden="true">↳</span><p>정의를 떠올리고, 차이를 짚고, 예시를 연결해 보세요.<br /><span>짧아도 괜찮습니다. 설명할 수 있는 만큼부터 시작하세요.</span></p></div>
    </section>
  </main>
</template>
