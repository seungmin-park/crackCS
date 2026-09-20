<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchAdminEvaluation, fetchAdminEvaluations, type AdminEvaluationDetail,
  type AdminEvaluationStatus, type AdminEvaluationSummary } from "@/api/admin/evaluations";
import AdminPagination from "@/components/AdminPagination.vue";
import { ADMIN_PAGE_SIZE, normalizedPage, queryPage, queryStringValue, replaceAdminQuery, updateAdminQuery } from "./adminPagination";

const route = useRoute();
const router = useRouter();
const status = ref<AdminEvaluationStatus | "">(queryStringValue(route.query, "status") as AdminEvaluationStatus | "");
const page = ref(queryPage(route.query));
const totalPages = ref(0);
const totalElements = ref(0);
const evaluations = ref<AdminEvaluationSummary[]>([]);
const selected = ref<AdminEvaluationDetail>();
const loading = ref(false);
const listError = ref(false);
const detailError = ref(false);
let listGeneration = 0;
let detailGeneration = 0;

async function load() {
  const generation = ++listGeneration;
  detailGeneration++;
  loading.value = true; listError.value = false; detailError.value = false; selected.value = undefined;
  try {
    const result = await fetchAdminEvaluations({ status: status.value, page: page.value, size: ADMIN_PAGE_SIZE });
    if (generation === listGeneration) {
      const validPage = normalizedPage(page.value, result.totalPages);
      if (validPage !== page.value) {
        await replaceAdminQuery(router, route.query, { page: validPage });
        return;
      }
      evaluations.value = result.content; page.value = result.page;
      totalPages.value = result.totalPages; totalElements.value = result.totalElements;
    }
  } catch {
    if (generation === listGeneration) listError.value = true;
  } finally {
    if (generation === listGeneration) loading.value = false;
  }
}

async function changeFilter() {
  await updateAdminQuery(router, route.query, { page: 0, status: status.value || undefined });
}

async function changePage(nextPage: number) {
  await updateAdminQuery(router, route.query, { page: nextPage, status: status.value || undefined });
}
watch(() => route.query, () => {
  status.value = queryStringValue(route.query, "status") as AdminEvaluationStatus | "";
  page.value = queryPage(route.query);
  void load();
}, { deep: true });

async function select(evaluationId: number) {
  const generation = ++detailGeneration;
  detailError.value = false;
  try {
    const result = await fetchAdminEvaluation(evaluationId);
    if (generation === detailGeneration) selected.value = result;
  } catch {
    if (generation === detailGeneration) detailError.value = true;
  }
}

onMounted(load);
onBeforeUnmount(() => { listGeneration++; detailGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">EVALUATIONS</p><h1>평가 검토</h1></div><p>자동 평가가 확정하지 못한 항목과 안전한 실패 코드를 확인합니다.</p></header>
    <div class="admin-toolbar"><label>상태 <select v-model="status" @change="changeFilter"><option value="">전체</option><option value="FAILED">실패</option><option value="NEEDS_REVIEW">검토 필요</option></select></label></div>
    <p v-if="listError" class="admin-error">평가 목록을 불러오지 못했습니다. <button type="button" data-retry="list" @click="load">다시 시도</button></p>
    <p v-if="detailError" class="admin-error">평가 상세를 불러오지 못했습니다.</p>
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
    <AdminPagination :page="page" :total-pages="totalPages" :total-elements="totalElements" @change="changePage" />
  </section>
</template>
