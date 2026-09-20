<script setup lang="ts">
import { computed, onBeforeUnmount, shallowRef, watch } from "vue";
import { useRouter } from "vue-router";

import { submitAnswer, type AnswerResponse, type FollowUpQuestionReason } from "@/api/answers";
import { difficultyLabel } from "@/api/questions";
import { useAnswerSubmission } from "@/composables/useAnswerSubmission";
import { useAuth } from "@/composables/useAuth";
import { useFollowUpQuestion } from "@/composables/useFollowUpQuestion";
import QuestionState from "@/components/QuestionState.vue";

const props = defineProps<{ answerId: number | string }>();
const router = useRouter();
const { currentMember } = useAuth();
const answerId = computed(() => props.answerId);
const followUp = useFollowUpQuestion(answerId);
const question = computed(() => followUp.result.value?.status === "READY" ? followUp.result.value.question : undefined);
const submission = shallowRef<ReturnType<typeof useAnswerSubmission<AnswerResponse>>>();
let submissionGeneration = 0;
let disposed = false;

watch([() => question.value?.id, () => currentMember.value?.id], ([questionId, memberId]) => {
  submissionGeneration += 1;
  submission.value = questionId === undefined ? undefined : useAnswerSubmission(memberId, questionId, submitAnswer);
}, { immediate: true });

async function handleSubmit(retry = false) {
  const activeGeneration = submissionGeneration;
  const activeSubmission = submission.value;
  if (!activeSubmission) return;

  const answer = retry ? await activeSubmission.retry() : await activeSubmission.submit();
  if (answer && !disposed && activeGeneration === submissionGeneration) {
    await router.push({ name: "answer-detail", params: { answerId: answer.answerId } });
  }
}

function failureDescription(reason: FollowUpQuestionReason): string {
  return {
    PROVIDER_TIMEOUT: "생성 시간이 초과됐어요. 다른 기본 문제로 학습을 이어갈 수 있어요.",
    INVALID_RESULT: "안전하게 표시할 수 있는 질문이 만들어지지 않았어요.",
    PROVIDER_ERROR: "질문 생성 서비스가 응답하지 않았어요.",
    ATTEMPTS_EXHAUSTED: "여러 번 시도했지만 질문을 완성하지 못했어요.",
    PERSISTENCE_ERROR: "완성된 질문을 저장하지 못했어요.",
    EVALUATION_NOT_ELIGIBLE: "현재 평가 결과는 후속 질문 생성 대상이 아니에요.",
    FOLLOW_UP_LIMIT: "후속 질문에는 또 다른 후속 질문을 만들지 않아요.",
    CONTENT_UNAVAILABLE: "공개 중인 학습 자료만으로 질문을 만들 수 없었어요.",
  }[reason];
}

onBeforeUnmount(() => {
  disposed = true;
  submissionGeneration += 1;
});
</script>

<template>
  <section class="follow-up-panel" aria-labelledby="follow-up-heading">
    <p class="eyebrow">다음 학습</p>
    <h2 id="follow-up-heading">후속 질문</h2>

    <QuestionState
      v-if="followUp.error.value?.retryable"
      kind="error"
      title="후속 질문을 확인하지 못했어요"
      description="잠시 후 같은 답변에서 다시 확인해 주세요."
      action-label="다시 조회"
      @action="followUp.retry"
    />

    <QuestionState
      v-else-if="followUp.error.value"
      kind="error"
      :title="followUp.error.value.kind === 'not-found' ? '후속 질문을 찾을 수 없어요' : followUp.error.value.kind === 'forbidden' ? '후속 질문에 접근할 수 없어요' : '후속 질문을 확인하지 못했어요'"
      description="답변 소유권과 로그인 상태를 확인해 주세요."
    />

    <div v-else-if="followUp.loading.value" class="follow-up-status" role="status" aria-busy="true">
      <strong>후속 질문을 확인하고 있어요</strong>
      <p>평가 결과에 맞는 다음 학습을 찾고 있습니다.</p>
    </div>

    <div v-else-if="followUp.result.value?.status === 'PENDING'" class="follow-up-status" role="status" aria-busy="true">
      <strong>후속 질문을 준비하고 있어요</strong>
      <p>평가 결과를 바탕으로 생성 작업을 기다리고 있습니다.</p>
    </div>

    <div v-else-if="followUp.result.value?.status === 'PROCESSING'" class="follow-up-status" role="status" aria-busy="true">
      <strong>후속 질문을 만들고 있어요</strong>
      <p>잠시만 기다리면 이 화면에서 바로 이어서 풀 수 있어요.</p>
    </div>

    <div v-else-if="followUp.result.value?.status === 'FAILED'" class="follow-up-status" role="status">
      <strong>후속 질문을 만들지 못했어요</strong>
      <p>{{ failureDescription(followUp.result.value.reason) }}</p>
      <RouterLink to="/questions">다른 기본 문제 풀기 →</RouterLink>
    </div>

    <div v-else-if="followUp.result.value?.status === 'UNAVAILABLE' && followUp.result.value.reason === 'FOLLOW_UP_LIMIT'" class="follow-up-status" role="status">
      <strong>후속 학습을 마쳤어요</strong>
      <p>이번 답변 평가가 끝나면 다음 기본 문제로 학습을 이어가세요.</p>
      <RouterLink to="/questions">다음 기본 문제 찾기 →</RouterLink>
    </div>

    <div v-else-if="followUp.result.value?.status === 'UNAVAILABLE'" class="follow-up-status" role="status">
      <strong>이번 답변에는 후속 질문이 없어요</strong>
      <p>{{ failureDescription(followUp.result.value.reason) }}</p>
      <RouterLink to="/questions">다른 기본 문제 풀기 →</RouterLink>
    </div>

    <div v-else-if="question" class="follow-up-ready">
      <div class="follow-up-question">
        <div class="detail-meta">
          <span>{{ question.topic.name }}</span>
          <span class="difficulty-pill" :data-difficulty="question.difficulty">{{ difficultyLabel(question.difficulty) }}</span>
        </div>
        <p>{{ question.content }}</p>
      </div>

      <form v-if="currentMember?.role === 'USER' && submission" class="answer-form" @submit.prevent="handleSubmit(false)">
        <label :for="`follow-up-answer-${question.id}`">내 후속 답변</label>
        <textarea
          :id="`follow-up-answer-${question.id}`"
          v-model="submission.content.value"
          maxlength="10000"
          rows="7"
          placeholder="앞선 피드백을 떠올리며 자신의 언어로 설명해 보세요."
        />
        <div class="answer-form-meta">
          <span>{{ submission.content.value.length.toLocaleString() }} / 10,000자</span>
          <button type="submit" :disabled="!submission.canSubmit.value">{{ submission.submitting.value ? "제출 중…" : "후속 답변 제출" }}</button>
        </div>
        <p v-if="submission.error.value" class="form-error" role="alert">{{ submission.error.value }}</p>
        <div v-if="submission.pendingPayload.value" class="pending-submission">
          <p>결과가 확인되지 않은 제출이 있습니다. 당시 답변을 그대로 다시 확인할 수 있습니다.</p>
          <button type="button" :disabled="submission.submitting.value" @click="handleSubmit(true)">같은 답변으로 다시 확인</button>
        </div>
      </form>
    </div>
  </section>
</template>
