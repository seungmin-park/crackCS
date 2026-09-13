<script setup lang="ts">
import { onBeforeUnmount, ref } from "vue";
import { fetchProgress, type LearningProgress, type RecentEvaluation } from "@/api/learning";
import KnowledgeTopics from "@/components/KnowledgeTopics.vue";
import QuestionState from "@/components/QuestionState.vue";

const progress = ref<LearningProgress>();
const loading = ref(true);
const failed = ref(false);
let active = true;
onBeforeUnmount(() => { active = false; });
function resultLabel(value: RecentEvaluation) {
  if (value.status === "FAILED") return "평가 실패";
  if (value.status === "NEEDS_REVIEW") return "검토 필요";
  if (value.status !== "EVALUATED") return "평가 중";
  return value.verdict === "CORRECT" ? "정답" : value.verdict === "PARTIALLY_CORRECT" ? "부분 정답" : value.verdict === "INCORRECT" ? "오답" : "검토 필요";
}
async function load() {
  loading.value = true;
  failed.value = false;
  try {
    const result = await fetchProgress();
    if (active) progress.value = result;
  } catch {
    if (active) failed.value = true;
  } finally {
    if (active) loading.value = false;
  }
}
void load();
</script>

<template>
  <main class="learning-shell">
    <header class="page-intro"><p class="eyebrow">한 질문씩, 내 언어로</p><h1>오늘의 학습</h1><p>아직 살펴보지 않은 개념부터, 다시 설명하고 싶은 개념까지.</p></header>
    <p v-if="loading" role="status" aria-busy="true">학습 현황을 불러오는 중…</p>
    <QuestionState v-else-if="failed" title="학습 현황을 불러오지 못했어요" description="잠시 후 다시 시도해 주세요." action-label="다시 불러오기" @action="load" />
    <template v-else-if="progress">
      <section class="learning-summary" aria-label="풀이 현황"><p>전체 풀이 {{ progress.totalAnswers }}회</p><p>최근 7일 {{ progress.recentAnswerCount }}회</p></section>
      <p v-if="progress.totalAnswers === 0" class="learning-welcome">첫 답변을 남겨 나의 지식 지도를 채워 보세요.</p>
      <section class="learning-recommendation" aria-labelledby="recommendation-heading">
        <p class="eyebrow">다음 한 걸음</p><h2 id="recommendation-heading">다음 추천 문제</h2>
        <p>{{ progress.recommendation.reasonText }}</p>
        <p v-if="progress.recommendation.conceptName">먼저 살펴볼 개념: {{ progress.recommendation.conceptName }}</p>
        <RouterLink v-if="progress.recommendation.questionId !== null" class="recommendation-link" :to="`/questions/${progress.recommendation.questionId}`">{{ progress.recommendation.title }} <span aria-hidden="true">↗</span></RouterLink>
        <RouterLink v-else class="text-link" to="/questions">문제집 둘러보기</RouterLink>
      </section>
      <section class="learning-section" aria-labelledby="topics-heading">
        <div class="section-heading"><h2 id="topics-heading">주제별 학습 상태</h2><RouterLink class="text-link" to="/knowledge-map">지식 지도 보기 →</RouterLink></div>
        <KnowledgeTopics :topics="progress.topics" />
      </section>
      <section class="learning-section" aria-labelledby="recent-heading">
        <div class="section-heading"><h2 id="recent-heading">최근 평가</h2><RouterLink class="text-link" to="/answers">답변 이력 →</RouterLink></div>
        <p v-if="!progress.recentEvaluations.length">아직 평가할 답변이 없어요.</p>
        <ul v-else class="recent-evaluations">
          <li v-for="evaluation in progress.recentEvaluations" :key="evaluation.answerId">
            <RouterLink :to="`/answers/${evaluation.answerId}`"><span>{{ evaluation.questionTitle }}</span><span>{{ resultLabel(evaluation) }}<template v-if="evaluation.score !== null"> · {{ evaluation.score }}점</template></span></RouterLink>
          </li>
        </ul>
      </section>
    </template>
  </main>
</template>
