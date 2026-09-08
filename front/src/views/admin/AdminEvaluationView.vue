<script setup lang="ts">
import { onMounted, ref } from "vue";
import { fetchAdminEvaluation, fetchAdminEvaluations, type AdminEvaluationDetail,
  type AdminEvaluationStatus, type AdminEvaluationSummary } from "@/api/admin";

const status = ref<AdminEvaluationStatus | "">("");
const evaluations = ref<AdminEvaluationSummary[]>([]);
const selected = ref<AdminEvaluationDetail>();
const loading = ref(false);
const error = ref(false);

async function load() {
  loading.value = true; error.value = false; selected.value = undefined;
  try { evaluations.value = (await fetchAdminEvaluations({ status: status.value, page: 0, size: 20 })).content; }
  catch { error.value = true; }
  finally { loading.value = false; }
}

async function select(evaluationId: number) {
  error.value = false;
  try { selected.value = await fetchAdminEvaluation(evaluationId); }
  catch { error.value = true; }
}

onMounted(load);
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">EVALUATIONS</p><h1>평가 검토</h1></div><p>자동 평가가 확정하지 못한 항목과 안전한 실패 코드를 확인합니다.</p></header>
    <div class="admin-toolbar"><label>상태 <select v-model="status" @change="load"><option value="">전체</option><option value="FAILED">실패</option><option value="NEEDS_REVIEW">검토 필요</option></select></label></div>
    <p v-if="error" class="admin-error">평가 정보를 불러오지 못했습니다.</p>
    <p v-if="loading" class="admin-loading">평가를 불러오는 중…</p>
    <div v-else class="admin-editor-layout">
      <ul class="admin-list selectable"><li v-for="item in evaluations" :key="item.evaluationId">
        <button type="button" :data-evaluation-id="item.evaluationId" @click="select(item.evaluationId)">
          <strong>#{{ item.evaluationId }} · {{ item.status }}</strong><small>{{ item.failureCode || "판정 검토" }}</small>
        </button>
      </li></ul>
      <section v-if="selected" class="admin-panel">
        <p class="eyebrow">{{ selected.status }}</p><h2>{{ selected.failureCode || "판정 검토" }}</h2>
        <h3>질문</h3><p>{{ selected.questionContent }}</p><h3>답변 원문</h3><p>{{ selected.answerContent }}</p>
        <p class="admin-meta">{{ selected.modelName || "모델 호출 전" }} · {{ selected.evaluatorVersion || "버전 없음" }}</p>
        <section v-if="selected.evidence.length"><h3>검색 근거</h3><article v-for="item in selected.evidence" :key="item.chunkId"><strong>{{ item.documentTitle }} · v{{ item.documentVersion }} · {{ item.startOffset }}–{{ item.endOffset }}</strong><p>{{ item.content }}</p></article></section>
      </section>
      <p v-else>목록에서 검토할 평가를 선택하세요.</p>
    </div>
  </section>
</template>
