<script setup lang="ts">
import RequestFailure from "@/components/RequestFailure.vue";
import { presentRequestError, type RequestErrorPresentation } from "@/presentation/requestErrorPresentation";

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
const selectedEvaluation = ref<AdminEvaluationDetail>();
const loading = ref(false);
const listError = ref<RequestErrorPresentation>();
const detailError = ref<RequestErrorPresentation>();
const detailLoading = ref(false);
const selectedEvaluationId = ref<number>();
let listGeneration = 0;
let detailGeneration = 0;

async function loadEvaluations() {
  const generation = ++listGeneration;
  detailGeneration++;
  detailLoading.value = false; selectedEvaluationId.value = undefined;
  loading.value = true; listError.value = undefined; detailError.value = undefined; selectedEvaluation.value = undefined;
  try {
    const evaluationPage = await fetchAdminEvaluations({ status: status.value, page: page.value, size: ADMIN_PAGE_SIZE });
    if (generation === listGeneration) {
      const validPage = normalizedPage(page.value, evaluationPage.totalPages);
      if (validPage !== page.value) {
        await replaceAdminQuery(router, route.query, { page: validPage });
        return;
      }
      evaluations.value = evaluationPage.content; page.value = evaluationPage.page;
      totalPages.value = evaluationPage.totalPages; totalElements.value = evaluationPage.totalElements;
    }
  } catch (caught) {
    if (generation === listGeneration) listError.value = presentRequestError(caught);
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
  void loadEvaluations();
}, { deep: true });

async function selectEvaluation(evaluationId: number) {
  const generation = ++detailGeneration;
  detailError.value = undefined;
  selectedEvaluation.value = undefined;
  selectedEvaluationId.value = evaluationId;
  detailLoading.value = true;
  try {
    const evaluation = await fetchAdminEvaluation(evaluationId);
    if (generation === detailGeneration) selectedEvaluation.value = evaluation;
  } catch (caught) {
    if (generation === detailGeneration) detailError.value = presentRequestError(caught);
  } finally {
    if (generation === detailGeneration) detailLoading.value = false;
  }
}

onMounted(loadEvaluations);
onBeforeUnmount(() => { listGeneration++; detailGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">EVALUATIONS</p><h1>평가 검토</h1></div><p>자동 평가가 확정하지 못한 항목과 안전한 실패 코드를 확인합니다.</p></header>
    <div class="admin-toolbar"><label>상태 <select v-model="status" @change="changeFilter"><option value="">전체</option><option value="FAILED">실패</option><option value="NEEDS_REVIEW">검토 필요</option></select></label></div>
    <RequestFailure v-if="listError" :failure="listError" title="평가 목록을 불러오지 못했습니다." retry-key="list" @retry="loadEvaluations" />
    <RequestFailure v-if="detailError" :failure="detailError" title="평가 상세를 불러오지 못했습니다." retry-key="detail" @retry="selectedEvaluationId !== undefined && selectEvaluation(selectedEvaluationId)" />
    <p v-if="detailLoading" role="status" aria-busy="true">평가 상세를 불러오는 중…</p>
    <p v-if="loading" role="status" aria-busy="true" class="admin-loading">평가를 불러오는 중…</p>
    <div v-else-if="!listError" class="admin-editor-layout">
      <p v-if="!evaluations.length" role="status">조건에 맞는 평가가 없습니다.</p>
      <ul v-else class="admin-list selectable"><li v-for="evaluation in evaluations" :key="evaluation.evaluationId">
        <button type="button" :data-evaluation-id="evaluation.evaluationId" @click="selectEvaluation(evaluation.evaluationId)">
          <strong>#{{ evaluation.evaluationId }} · {{ evaluation.status }}</strong><small>{{ evaluation.failureCode || "판정 검토" }}</small>
        </button>
      </li></ul>
      <section v-if="selectedEvaluation" class="admin-panel">
        <p class="eyebrow">{{ selectedEvaluation.status }}</p><h2>{{ selectedEvaluation.failureCode || "판정 검토" }}</h2>
        <h3>질문</h3><p>{{ selectedEvaluation.questionContent }}</p><h3>답변 원문</h3><p>{{ selectedEvaluation.answerContent }}</p>
        <p class="admin-meta">{{ selectedEvaluation.modelName || "모델 호출 전" }} · {{ selectedEvaluation.evaluatorVersion || "버전 없음" }}</p>
        <section v-if="selectedEvaluation.evidence.length"><h3>검색 근거</h3><article v-for="evidence in selectedEvaluation.evidence" :key="evidence.chunkId"><strong>{{ evidence.documentTitle }} · v{{ evidence.documentVersion }} · {{ evidence.startOffset }}–{{ evidence.endOffset }}</strong><p>{{ evidence.content }}</p></article></section>
      </section>
      <p v-else-if="!detailLoading && !detailError && evaluations.length">목록에서 검토할 평가를 선택하세요.</p>
    </div>
    <AdminPagination v-if="!loading && !listError" :page="page" :total-pages="totalPages" :total-elements="totalElements" @change="changePage" />
  </section>
</template>
