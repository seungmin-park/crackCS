<script setup lang="ts">
import { onBeforeUnmount, ref } from "vue";
import { fetchKnowledgeStates, type TopicKnowledge } from "@/api/learning";
import KnowledgeTopics from "@/components/KnowledgeTopics.vue";
import QuestionState from "@/components/QuestionState.vue";

const topics = ref<TopicKnowledge[]>([]);
const loading = ref(true);
const failed = ref(false);
let active = true;
onBeforeUnmount(() => { active = false; });

async function load() {
  loading.value = true;
  failed.value = false;
  try {
    const result = await fetchKnowledgeStates();
    if (active) topics.value = result.topics;
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
    <header class="page-intro"><p class="eyebrow">내가 설명할 수 있는 것들</p><h1>나의 지식 지도</h1><p>미평가는 아직 확인하지 않은 개념이에요. 낮은 점수와는 다릅니다.</p></header>
    <aside class="knowledge-guide" aria-label="지식 상태 읽는 법">
      <p><strong>숙련도</strong>는 지금까지의 판정과 가장 최근 판정을 반영한 점수입니다.</p>
      <p><strong>신뢰도</strong>는 평가가 얼마나 쌓였는지 나타냅니다. AI의 정답 확률이 아닙니다.</p>
      <p>숙련도 80 이상·신뢰도 75 이상이면 안정 상태예요. 두 점수의 범위는 0~100입니다.</p>
    </aside>
    <p v-if="loading" role="status" aria-busy="true">지식 지도를 불러오는 중…</p>
    <QuestionState v-else-if="failed" title="지식 지도를 불러오지 못했어요" description="잠시 후 다시 시도해 주세요." action-label="다시 불러오기" @action="load" />
    <KnowledgeTopics v-else :topics="topics" details />
  </main>
</template>
