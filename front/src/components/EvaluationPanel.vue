<script setup lang="ts">
import { computed } from "vue";
import type { AnswerEvaluation, EvaluationVerdict } from "@/api/answers";

const props = defineProps<{ evaluation: AnswerEvaluation }>();
const labels: Record<EvaluationVerdict, string> = { CORRECT: "정답", PARTIALLY_CORRECT: "부분 정답", INCORRECT: "오답", NEEDS_REVIEW: "검토 필요" };
const verdictLabel = computed(() => props.evaluation.verdict ? labels[props.evaluation.verdict] : "");
</script>

<template>
  <section class="evaluation-panel" :data-status="evaluation.status">
    <template v-if="evaluation.status === 'EVALUATING' || evaluation.status === 'PROCESSING'">
      <p class="eyebrow">평가 중</p><h2>답변을 살펴보고 있어요</h2><p>페이지를 닫아도 평가는 계속됩니다. 다시 열면 현재 상태부터 확인합니다.</p>
    </template>
    <template v-else-if="evaluation.status === 'FAILED'">
      <p class="eyebrow">평가 실패</p><h2>지금은 평가를 완료하지 못했어요</h2><p>답변이 틀렸다는 뜻은 아닙니다. 답변 이력에서 원문을 확인한 뒤 새 답변으로 다시 제출해 주세요.</p><small v-if="evaluation.failureReason">{{ evaluation.failureReason }}</small>
    </template>
    <template v-else-if="evaluation.status === 'NEEDS_REVIEW'">
      <p class="eyebrow">검토 필요</p><h2>사람의 검토가 필요합니다</h2>
      <p>근거가 충분하지 않거나 서로 충돌합니다. 학습 상태에는 반영되지 않습니다.</p>
      <small v-if="evaluation.failureReason">{{ evaluation.failureReason }}</small>
    </template>
    <template v-else>
      <p class="eyebrow">평가 결과</p><h2>{{ verdictLabel }}<span v-if="evaluation.score !== null"> · {{ evaluation.score }}점</span></h2>
      <p v-if="evaluation.verdict === 'NEEDS_REVIEW'">근거가 충분하지 않아 사람의 검토가 필요합니다. 학습 상태에는 반영되지 않습니다.</p>
      <p v-if="evaluation.feedback">{{ evaluation.feedback }}</p>
      <ul v-if="evaluation.concepts.length" class="concept-results"><li v-for="concept in evaluation.concepts" :key="concept.conceptId"><strong>{{ concept.conceptName }} · {{ labels[concept.verdict] }}</strong><span v-if="concept.feedback">{{ concept.feedback }}</span></li></ul>
      <section v-if="evaluation.strengths?.length"><h3>잘 설명한 점</h3><ul><li v-for="item in evaluation.strengths" :key="item">{{ item }}</li></ul></section>
      <section v-if="evaluation.omissions?.length"><h3>빠진 점</h3><ul><li v-for="item in evaluation.omissions" :key="item">{{ item }}</li></ul></section>
      <section v-if="evaluation.misconceptions?.length"><h3>바로잡을 점</h3><ul><li v-for="item in evaluation.misconceptions" :key="item">{{ item }}</li></ul></section>
      <section v-if="evaluation.evidence?.length" class="evaluation-evidence">
        <h3>판정 근거</h3>
        <article v-for="item in evaluation.evidence" :key="item.chunkId">
          <strong>{{ item.documentTitle }} · v{{ item.documentVersion }} · {{ item.startOffset }}–{{ item.endOffset }}</strong>
          <p>{{ item.content }}</p>
        </article>
      </section>
    </template>
  </section>
</template>
