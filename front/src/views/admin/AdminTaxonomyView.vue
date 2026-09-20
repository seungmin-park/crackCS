<script setup lang="ts">
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
import { ADMIN_PAGE_SIZE, fetchAllPages, queryPage, updateAdminQuery } from "./adminPagination";

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
const relationError = ref(false);
const loading = ref(true);
const topicEditingId = ref<number>();
const conceptEditingId = ref<number>();
const topicForm = reactive({ parentId: "", code: "", name: "" });
const conceptForm = reactive({ topicId: "", code: "", name: "", description: "" });
const feedback = useAdminFeedback();
const loadError = ref(false);
let loadGeneration = 0;
let relationGeneration = 0;

async function load() {
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = false;
  try {
    const [topicResult, conceptResult] = await Promise.all([
      fetchTopics({ page: topicPage.value, size: ADMIN_PAGE_SIZE }),
      fetchConcepts({ page: conceptPage.value, size: ADMIN_PAGE_SIZE }),
    ]);
    if (generation !== loadGeneration) return;
    topics.value = topicResult.content;
    concepts.value = conceptResult.content;
    topicPage.value = topicResult.page; topicTotalPages.value = topicResult.totalPages; topicTotalElements.value = topicResult.totalElements;
    conceptPage.value = conceptResult.page; conceptTotalPages.value = conceptResult.totalPages; conceptTotalElements.value = conceptResult.totalElements;
  } catch {
    if (generation === loadGeneration) loadError.value = true;
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}

async function loadRelations() {
  const generation = ++relationGeneration;
  relationLoading.value = true; relationError.value = false;
  try {
    const result = await fetchAllPages((candidatePage, size) => fetchTopics({ active: true, page: candidatePage, size }));
    if (generation === relationGeneration) topicOptions.value = result;
  } catch { if (generation === relationGeneration) relationError.value = true; }
  finally { if (generation === relationGeneration) relationLoading.value = false; }
}

async function changePage(key: "topicPage" | "conceptPage", nextPage: number) {
  await updateAdminQuery(router, route.query, { [key]: nextPage });
}

watch(() => route.query, () => {
  topicPage.value = queryPage(route.query, "topicPage");
  conceptPage.value = queryPage(route.query, "conceptPage");
  void load();
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
  const result = await feedback.execute(
    () => topicEditingId.value ? updateTopic(topicEditingId.value, input) : createTopic(input),
    topicEditingId.value ? "Topic을 수정했습니다." : "Topic을 등록했습니다.",
  );
  if (result) { topicEditingId.value = undefined; Object.assign(topicForm, { parentId: "", code: "", name: "" }); await load(); }
}

async function submitConcept() {
  const input = { topicId: Number(conceptForm.topicId), code: conceptForm.code, name: conceptForm.name, description: conceptForm.description };
  const result = await feedback.execute(
    () => conceptEditingId.value ? updateConcept(conceptEditingId.value, input) : createConcept(input),
    conceptEditingId.value ? "Concept을 수정했습니다." : "Concept을 등록했습니다.",
  );
  if (result) { conceptEditingId.value = undefined; Object.assign(conceptForm, { topicId: "", code: "", name: "", description: "" }); await load(); }
}

async function deactivate(kind: "topic" | "concept", id: number) {
  const result = await feedback.execute(
    () => kind === "topic" ? deactivateTopic(id) : deactivateConcept(id),
    `${kind === "topic" ? "Topic" : "Concept"}을 비활성화했습니다.`,
  );
  if (result === undefined && feedback.formError.value) return;
  await load();
}

onMounted(() => { void load(); void loadRelations(); });
onBeforeUnmount(() => { loadGeneration++; relationGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">TAXONOMY</p><h1>분류와 개념</h1></div><p>비활성 분류는 기존 이력을 보존하지만 새 콘텐츠에는 연결할 수 없습니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" />
    <p v-if="relationError" class="admin-error">관계 후보를 불러오지 못했습니다. <button type="button" data-retry="relations" @click="loadRelations">다시 시도</button></p>
    <p v-else-if="relationLoading" class="admin-loading">관계 후보를 불러오는 중…</p>
    <p v-if="loadError" class="admin-error">분류 체계를 불러오지 못했습니다. <button type="button" data-retry="list" @click="load">다시 시도</button></p>
    <p v-if="loading" class="admin-loading">분류 체계를 불러오는 중…</p>
    <div v-else class="admin-two-column">
      <section class="admin-panel">
        <h2>Topic</h2>
        <form class="admin-form" @submit.prevent="submitTopic">
          <label>상위 Topic<select v-model="topicForm.parentId"><option value="">없음</option><option v-for="item in topicOptions.filter(t => t.active && t.id !== topicEditingId)" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
          <label>코드<input v-model="topicForm.code" required /><small>{{ feedback.fieldErrors.value.code }}</small></label>
          <label>이름<input v-model="topicForm.name" required /><small>{{ feedback.fieldErrors.value.name }}</small></label>
          <button class="admin-primary" :disabled="feedback.submitting.value">{{ topicEditingId ? "Topic 수정" : "Topic 등록" }}</button>
        </form>
        <ul class="admin-list"><li v-for="item in topics" :key="item.id" :class="{ inactive: !item.active }"><div><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.active ? "ACTIVE" : "INACTIVE" }}</small></div><div><button :disabled="feedback.submitting.value" @click="editTopic(item)">편집</button><button v-if="item.active" :disabled="feedback.submitting.value" @click="deactivate('topic', item.id)">비활성화</button></div></li></ul>
        <AdminPagination :page="topicPage" :total-pages="topicTotalPages" :total-elements="topicTotalElements" data-page-key="topicPage" @change="changePage('topicPage', $event)" />
      </section>
      <section class="admin-panel">
        <h2>Concept</h2>
        <form class="admin-form" @submit.prevent="submitConcept">
          <label>Topic<select v-model="conceptForm.topicId" required><option value="" disabled>선택</option><option v-for="item in topicOptions.filter(t => t.active)" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
          <label>코드<input v-model="conceptForm.code" required /></label><label>이름<input v-model="conceptForm.name" required /></label>
          <label>설명<textarea v-model="conceptForm.description" rows="3" /></label>
          <button class="admin-primary" :disabled="feedback.submitting.value">{{ conceptEditingId ? "Concept 수정" : "Concept 등록" }}</button>
        </form>
        <ul class="admin-list"><li v-for="item in concepts" :key="item.id" :class="{ inactive: !item.active }"><div><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.active ? "ACTIVE" : "INACTIVE" }}</small></div><div><button :disabled="feedback.submitting.value" @click="editConcept(item)">편집</button><button v-if="item.active" :disabled="feedback.submitting.value" @click="deactivate('concept', item.id)">비활성화</button></div></li></ul>
        <AdminPagination :page="conceptPage" :total-pages="conceptTotalPages" :total-elements="conceptTotalElements" data-page-key="conceptPage" @change="changePage('conceptPage', $event)" />
      </section>
    </div>
  </section>
</template>
