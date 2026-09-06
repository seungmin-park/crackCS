<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";

import { ApiClientError } from "@/api/client";
import { difficultyLabel, fetchQuestion, type PublicQuestion } from "@/api/questions";
import QuestionState from "@/components/QuestionState.vue";

const route = useRoute();
const question = ref<PublicQuestion>();
const loading = ref(true);
const error = ref<"not-found" | "server">();
const questionId = computed(() => String(route.params.questionId));

async function loadQuestion() {
  loading.value = true;
  error.value = undefined;

  try {
    question.value = await fetchQuestion(questionId.value);
  } catch (caught) {
    error.value = caught instanceof ApiClientError && caught.status === 404 ? "not-found" : "server";
  } finally {
    loading.value = false;
  }
}

onMounted(loadQuestion);
</script>

<template>
  <main class="page-shell detail-shell">
    <RouterLink class="back-link" :to="{ path: '/questions', query: route.query }">← 문제 목록</RouterLink>

    <section v-if="loading" class="detail-card" aria-label="문제를 불러오는 중">
      <div class="skeleton-line short" />
      <div class="skeleton-line" />
      <div class="skeleton-line" />
    </section>

    <QuestionState
      v-else-if="error === 'not-found'"
      title="문제를 찾을 수 없어요"
      description="존재하지 않거나 지금은 공개되지 않은 문제입니다."
    />

    <QuestionState
      v-else-if="error === 'server'"
      title="문제를 불러오지 못했어요"
      description="잠시 후 다시 시도해 주세요."
      action-label="다시 불러오기"
      @action="loadQuestion"
    />

    <article v-else-if="question" class="detail-card">
      <div class="detail-workspace">
        <div class="detail-reading">
          <div class="detail-meta">
            <span>{{ question.topic.name }}</span>
            <span class="difficulty-pill" :data-difficulty="question.difficulty">{{ difficultyLabel(question.difficulty) }}</span>
          </div>
          <p class="eyebrow">질문 {{ question.id }}</p>
          <h1>{{ question.content }}</h1>
        </div>
        <aside class="thinking-guide" aria-label="생각을 정리하는 순서">
          <strong>내 언어로 설명하기</strong>
          <p>정답을 찾기 전에, 알고 있는 내용을 먼저 떠올려 보세요.</p>
          <ol><li>핵심 개념 정의하기</li><li>차이와 이유 짚어보기</li><li>구체적인 예시 연결하기</li></ol>
        </aside>
      </div>
      <div class="detail-bottom"><span>설명이 막히는 부분이 다음에 공부할 지점입니다.</span><RouterLink :to="{ path: '/questions', query: route.query }">다른 문제 살펴보기 →</RouterLink></div>
    </article>
  </main>
</template>
