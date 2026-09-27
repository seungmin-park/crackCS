<script setup lang="ts">
import RequestFailure from "@/components/RequestFailure.vue";
import { presentRequestError, type RequestErrorPresentation } from "@/presentation/requestErrorPresentation";

import { onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import {
  createConcept,
  deactivateConcept,
  fetchConcepts,
  updateConcept,
  type Concept,
} from "@/api/admin/concepts";
import {
  createTopic,
  deactivateTopic,
  fetchTopics,
  updateTopic,
  type Topic,
} from "@/api/admin/topics";
import AdminFeedback from "@/components/AdminFeedback.vue";
import AdminPagination from "@/components/AdminPagination.vue";
import { useAdminFeedback } from "@/composables/useAdminFeedback";
import { ADMIN_PAGE_SIZE, fetchAllPages, normalizedPage, queryPage, replaceAdminQuery, updateAdminQuery } from "./adminPagination";

const route = useRoute();
const router = useRouter();
const topics = ref<Topic[]>([]);
const concepts = ref<Concept[]>([]);
const topicOptions = ref<Topic[]>([]);
const topicPage = ref(queryPage(route.query, "topicPage"));
const conceptPage = ref(queryPage(route.query, "conceptPage"));
const topicTotalPages = ref(0);
const topicTotalElements = ref(0);
const conceptTotalPages = ref(0);
const conceptTotalElements = ref(0);
const relationLoading = ref(true);
const relationError = ref<RequestErrorPresentation>();
const loading = ref(true);
const topicEditingId = ref<number>();
const conceptEditingId = ref<number>();
const topicForm = reactive({ parentId: "", code: "", name: "" });
const conceptForm = reactive({ topicId: "", code: "", name: "", description: "" });
const feedback = useAdminFeedback();
const loadError = ref<RequestErrorPresentation>();
let loadGeneration = 0;
let relationGeneration = 0;
let disposed = false;

async function loadTaxonomy() {
  if (disposed) return;
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = undefined;
  try {
    const [topicResult, conceptResult] = await Promise.all([
      fetchTopics({ page: topicPage.value, size: ADMIN_PAGE_SIZE }),
      fetchConcepts({ page: conceptPage.value, size: ADMIN_PAGE_SIZE }),
    ]);
    if (generation !== loadGeneration) return;
    const validTopicPage = normalizedPage(topicPage.value, topicResult.totalPages);
    const validConceptPage = normalizedPage(conceptPage.value, conceptResult.totalPages);
    if (validTopicPage !== topicPage.value || validConceptPage !== conceptPage.value) {
      await replaceAdminQuery(router, route.query, { topicPage: validTopicPage, conceptPage: validConceptPage });
      return;
    }
    topics.value = topicResult.content;
    concepts.value = conceptResult.content;
    topicPage.value = topicResult.page; topicTotalPages.value = topicResult.totalPages; topicTotalElements.value = topicResult.totalElements;
    conceptPage.value = conceptResult.page; conceptTotalPages.value = conceptResult.totalPages; conceptTotalElements.value = conceptResult.totalElements;
  } catch (caught) {
    if (generation === loadGeneration) loadError.value = presentRequestError(caught);
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}

async function loadActiveTopics() {
  if (disposed) return;
  const generation = ++relationGeneration;
  relationLoading.value = true; relationError.value = undefined;
  try {
    const activeTopics = await fetchAllPages((candidatePage, size) => fetchTopics({ active: true, page: candidatePage, size }));
    if (generation === relationGeneration) topicOptions.value = activeTopics;
  } catch (caught) { if (generation === relationGeneration) relationError.value = presentRequestError(caught); }
  finally { if (generation === relationGeneration) relationLoading.value = false; }
}

async function changePage(key: "topicPage" | "conceptPage", nextPage: number) {
  await updateAdminQuery(router, route.query, { [key]: nextPage });
}

watch(() => route.query, () => {
  topicPage.value = queryPage(route.query, "topicPage");
  conceptPage.value = queryPage(route.query, "conceptPage");
  void loadTaxonomy();
}, { deep: true });

function editTopic(topic: Topic) {
  topicEditingId.value = topic.id;
  Object.assign(topicForm, { parentId: topic.parentId?.toString() ?? "", code: topic.code, name: topic.name });
}

function editConcept(concept: Concept) {
  conceptEditingId.value = concept.id;
  Object.assign(conceptForm, {
    topicId: String(concept.topicId), code: concept.code, name: concept.name, description: concept.description ?? "",
  });
}

async function submitTopic() {
  const input = {
    code: topicForm.code,
    name: topicForm.name,
    ...(topicForm.parentId ? { parentId: Number(topicForm.parentId) } : {}),
  };
  const savedTopic = await feedback.execute(
    () => topicEditingId.value ? updateTopic(topicEditingId.value, input) : createTopic(input),
    topicEditingId.value ? "Topic을 수정했습니다." : "Topic을 등록했습니다.",
  );
  if (savedTopic && !disposed) {
    topicEditingId.value = undefined;
    Object.assign(topicForm, { parentId: "", code: "", name: "" });
    await Promise.all([loadTaxonomy(), loadActiveTopics()]);
  }
}

async function submitConcept() {
  const input = { topicId: Number(conceptForm.topicId), code: conceptForm.code, name: conceptForm.name, description: conceptForm.description };
  const savedConcept = await feedback.execute(
    () => conceptEditingId.value ? updateConcept(conceptEditingId.value, input) : createConcept(input),
    conceptEditingId.value ? "Concept을 수정했습니다." : "Concept을 등록했습니다.",
  );
  if (savedConcept && !disposed) { conceptEditingId.value = undefined; Object.assign(conceptForm, { topicId: "", code: "", name: "", description: "" }); await loadTaxonomy(); }
}

async function deactivate(kind: "topic" | "concept", id: number) {
  const deactivatedEntry = await feedback.execute(
    () => kind === "topic" ? deactivateTopic(id) : deactivateConcept(id),
    `${kind === "topic" ? "Topic" : "Concept"}을 비활성화했습니다.`,
  );
  if (disposed || (deactivatedEntry === undefined && feedback.formError.value)) return;
  if (kind === "topic") await Promise.all([loadTaxonomy(), loadActiveTopics()]);
  else await loadTaxonomy();
}

onMounted(() => { void loadTaxonomy(); void loadActiveTopics(); });
onBeforeUnmount(() => { disposed = true; loadGeneration++; relationGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">TAXONOMY</p><h1>분류와 개념</h1></div><p>비활성 분류는 기존 이력을 보존하지만 새 콘텐츠에는 연결할 수 없습니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" :field-errors="feedback.fieldErrors.value" />
    <RequestFailure v-if="relationError" :failure="relationError" title="관계 후보를 불러오지 못했습니다." retry-key="relations" @retry="loadActiveTopics" />
    <p v-else-if="relationLoading" role="status" aria-busy="true" class="admin-loading">관계 후보를 불러오는 중…</p>
    <RequestFailure v-if="loadError" :failure="loadError" title="분류 체계를 불러오지 못했습니다." retry-key="list" @retry="loadTaxonomy" />
    <p v-if="loading" role="status" aria-busy="true" class="admin-loading">분류 체계를 불러오는 중…</p>
    <div v-else-if="!loadError" class="admin-two-column">
      <section class="admin-panel">
        <h2>Topic</h2>
        <form class="admin-form" @submit.prevent="submitTopic">
          <label>상위 Topic<select v-model="topicForm.parentId"><option value="">없음</option><option v-for="topic in topicOptions.filter(candidate => candidate.active && candidate.id !== topicEditingId)" :key="topic.id" :value="topic.id">{{ topic.name }}</option></select></label>
          <label>코드<input v-model="topicForm.code" required /><small>{{ feedback.fieldErrors.value.code }}</small></label>
          <label>이름<input v-model="topicForm.name" required /><small>{{ feedback.fieldErrors.value.name }}</small></label>
          <button class="admin-primary" :disabled="feedback.submitting.value">{{ topicEditingId ? "Topic 수정" : "Topic 등록" }}</button>
        </form>
        <p v-if="!topics.length" role="status">조건에 맞는 주제가 없습니다.</p>
        <ul v-else class="admin-list"><li v-for="topic in topics" :key="topic.id" :class="{ inactive: !topic.active }"><div><strong>{{ topic.name }}</strong><small>{{ topic.code }} · {{ topic.active ? "ACTIVE" : "INACTIVE" }}</small></div><div><button :disabled="feedback.submitting.value" @click="editTopic(topic)">편집</button><button v-if="topic.active" :disabled="feedback.submitting.value" @click="deactivate('topic', topic.id)">비활성화</button></div></li></ul>
        <AdminPagination v-if="!loading && !loadError" :page="topicPage" :total-pages="topicTotalPages" :total-elements="topicTotalElements" data-page-key="topicPage" @change="changePage('topicPage', $event)" />
      </section>
      <section class="admin-panel">
        <h2>Concept</h2>
        <form class="admin-form" @submit.prevent="submitConcept">
          <label>Topic<select v-model="conceptForm.topicId" required><option value="" disabled>선택</option><option v-for="topic in topicOptions.filter(candidate => candidate.active)" :key="topic.id" :value="topic.id">{{ topic.name }}</option></select></label>
          <label>코드<input v-model="conceptForm.code" required /></label><label>이름<input v-model="conceptForm.name" required /></label>
          <label>설명<textarea v-model="conceptForm.description" rows="3" /></label>
          <button class="admin-primary" :disabled="feedback.submitting.value">{{ conceptEditingId ? "Concept 수정" : "Concept 등록" }}</button>
        </form>
        <p v-if="!concepts.length" role="status">조건에 맞는 개념이 없습니다.</p>
        <ul v-else class="admin-list"><li v-for="concept in concepts" :key="concept.id" :class="{ inactive: !concept.active }"><div><strong>{{ concept.name }}</strong><small>{{ concept.code }} · {{ concept.active ? "ACTIVE" : "INACTIVE" }}</small></div><div><button :disabled="feedback.submitting.value" @click="editConcept(concept)">편집</button><button v-if="concept.active" :disabled="feedback.submitting.value" @click="deactivate('concept', concept.id)">비활성화</button></div></li></ul>
        <AdminPagination v-if="!loading && !loadError" :page="conceptPage" :total-pages="conceptTotalPages" :total-elements="conceptTotalElements" data-page-key="conceptPage" @change="changePage('conceptPage', $event)" />
      </section>
    </div>
  </section>
</template>
