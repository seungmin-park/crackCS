<script setup lang="ts">
import { onMounted, ref } from "vue";

import { difficultyLabel, fetchQuestions, type PublicQuestion } from "@/api/questions";
import QuestionState from "@/components/QuestionState.vue";

const questions = ref<PublicQuestion[]>([]);
const loading = ref(true);
const failed = ref(false);

async function loadQuestions() {
  loading.value = true;
  failed.value = false;

  try {
    questions.value = (await fetchQuestions()).content;
  } catch {
    failed.value = true;
  } finally {
    loading.value = false;
  }
}

onMounted(loadQuestions);
</script>

<template>
  <main class="page-shell">
    <section class="page-intro">
      <p class="eyebrow">QUESTION LIBRARY</p>
      <h1>개념을 설명하며<br />내 것으로 만드세요.</h1>
      <p>정답을 외우기보다, 질문 앞에서 생각을 말로 정리해 보세요.</p>
    </section>

    <section v-if="loading" class="question-grid" aria-label="문제 목록을 불러오는 중">
      <div v-for="index in 3" :key="index" class="question-card skeleton-card" />
    </section>

    <QuestionState
      v-else-if="failed"
      title="문제를 불러오지 못했어요"
      description="서버 연결을 확인한 뒤 다시 시도해 주세요."
      action-label="다시 불러오기"
      @action="loadQuestions"
    />

    <QuestionState
      v-else-if="questions.length === 0"
      title="아직 공개된 문제가 없어요"
      description="새로운 문제가 준비되면 이곳에 표시됩니다."
    />

    <section v-else aria-labelledby="question-list-title">
      <div class="section-heading">
        <h2 id="question-list-title">공개 문제</h2>
        <span>{{ questions.length }} questions</span>
      </div>
      <div class="question-grid">
        <RouterLink
          v-for="(question, index) in questions"
          :key="question.id"
          class="question-card"
          :to="`/questions/${question.id}`"
        >
          <div class="card-meta">
            <span class="question-number">Q{{ String(index + 1).padStart(2, "0") }}</span>
            <span class="difficulty-pill" :data-difficulty="question.difficulty">
              {{ difficultyLabel(question.difficulty) }}
            </span>
          </div>
          <h3>{{ question.content }}</h3>
          <div class="card-footer">
            <span>{{ question.topic.name }}</span>
            <span aria-hidden="true">→</span>
          </div>
        </RouterLink>
      </div>
    </section>
  </main>
</template>
