<script setup lang="ts">
import { computed, onBeforeUnmount, ref, shallowRef, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import { submitAnswer } from "@/api/answers";
import { ApiClientError } from "@/api/client";
import { difficultyLabel, fetchQuestion, type PublicQuestion } from "@/api/questions";
import QuestionState from "@/components/QuestionState.vue";
import { useAnswerSubmission } from "@/composables/useAnswerSubmission";
import { useAuth } from "@/composables/useAuth";

const route = useRoute();
const router = useRouter();
const { currentMember } = useAuth();
const question = ref<PublicQuestion>();
const loading = ref(true);
const error = ref<"not-found" | "server">();
const questionId = computed(() => String(route.params.questionId));
const submission = shallowRef(useAnswerSubmission(currentMember.value?.id, Number(questionId.value), submitAnswer));
let generation = 0;
let disposed = false;

async function handleSubmit(retry = false) {
  const activeGeneration = generation;
  const answer = retry ? await submission.value.retry() : await submission.value.submit();
  if (answer && !disposed && activeGeneration === generation) await router.push({ name: "answer-detail", params: { answerId: answer.answerId } });
}

async function loadQuestion() {
  const activeGeneration = ++generation;
  loading.value = true;
  error.value = undefined;

  try {
    const loaded = await fetchQuestion(questionId.value);
    if (!disposed && activeGeneration === generation) question.value = loaded;
  } catch (caught) {
    if (!disposed && activeGeneration === generation) error.value = caught instanceof ApiClientError && caught.status === 404 ? "not-found" : "server";
  } finally {
    if (!disposed && activeGeneration === generation) loading.value = false;
  }
}

watch([questionId, () => currentMember.value?.id], () => {
  submission.value = useAnswerSubmission(currentMember.value?.id, Number(questionId.value), submitAnswer);
  void loadQuestion();
}, { immediate: true });
onBeforeUnmount(() => { disposed = true; generation++; });
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
      <form v-if="currentMember?.role === 'USER'" class="answer-form" @submit.prevent="handleSubmit(false)">
        <label for="answer-content">내 답변</label>
        <textarea id="answer-content" v-model="submission.content.value" maxlength="10000" rows="9" placeholder="외운 문장보다 이해한 내용을 자신의 언어로 설명해 보세요." />
        <div class="answer-form-meta"><span>{{ submission.content.value.length.toLocaleString() }} / 10,000자</span><button type="submit" :disabled="!submission.canSubmit.value">{{ submission.submitting.value ? "제출 중…" : "답변 제출" }}</button></div>
        <p v-if="submission.error.value" class="form-error" role="alert">{{ submission.error.value }}</p>
        <div v-if="submission.pendingPayload.value" class="pending-submission">
          <p>결과가 확인되지 않은 제출이 있습니다. 당시 답변을 그대로 다시 확인할 수 있습니다.</p>
          <button type="button" :disabled="submission.submitting.value" @click="handleSubmit(true)">같은 답변으로 다시 확인</button>
        </div>
      </form>
      <div class="detail-bottom"><span>설명이 막히는 부분이 다음에 공부할 지점입니다.</span><RouterLink :to="{ path: '/questions', query: route.query }">다른 문제 살펴보기 →</RouterLink></div>
    </article>
  </main>
</template>
