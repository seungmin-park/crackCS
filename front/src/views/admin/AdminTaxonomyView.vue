<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";

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
import { useAdminFeedback } from "@/composables/useAdminFeedback";

const topics = ref<Topic[]>([]);
const concepts = ref<Concept[]>([]);
const loading = ref(true);
const topicEditingId = ref<number>();
const conceptEditingId = ref<number>();
const topicForm = reactive({ parentId: "", code: "", name: "" });
const conceptForm = reactive({ topicId: "", code: "", name: "", description: "" });
const feedback = useAdminFeedback();
const loadError = ref(false);
let loadGeneration = 0;

async function load() {
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = false;
  try {
    const [topicPage, conceptPage] = await Promise.all([fetchTopics({ size: 100 }), fetchConcepts({ size: 100 })]);
    if (generation !== loadGeneration) return;
    topics.value = topicPage.content;
    concepts.value = conceptPage.content;
  } catch {
    if (generation === loadGeneration) loadError.value = true;
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}

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

onMounted(load);
onBeforeUnmount(() => { loadGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">TAXONOMY</p><h1>분류와 개념</h1></div><p>비활성 분류는 기존 이력을 보존하지만 새 콘텐츠에는 연결할 수 없습니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" />
    <p v-if="loadError" class="admin-error">분류 체계를 불러오지 못했습니다. <button type="button" data-retry="list" @click="load">다시 시도</button></p>
    <p v-if="loading" class="admin-loading">분류 체계를 불러오는 중…</p>
    <div v-else class="admin-two-column">
      <section class="admin-panel">
        <h2>Topic</h2>
        <form class="admin-form" @submit.prevent="submitTopic">
          <label>상위 Topic<select v-model="topicForm.parentId"><option value="">없음</option><option v-for="item in topics.filter(t => t.active && t.id !== topicEditingId)" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
          <label>코드<input v-model="topicForm.code" required /><small>{{ feedback.fieldErrors.value.code }}</small></label>
          <label>이름<input v-model="topicForm.name" required /><small>{{ feedback.fieldErrors.value.name }}</small></label>
          <button class="admin-primary" :disabled="feedback.submitting.value">{{ topicEditingId ? "Topic 수정" : "Topic 등록" }}</button>
        </form>
        <ul class="admin-list"><li v-for="item in topics" :key="item.id" :class="{ inactive: !item.active }"><div><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.active ? "ACTIVE" : "INACTIVE" }}</small></div><div><button :disabled="feedback.submitting.value" @click="editTopic(item)">편집</button><button v-if="item.active" :disabled="feedback.submitting.value" @click="deactivate('topic', item.id)">비활성화</button></div></li></ul>
      </section>
      <section class="admin-panel">
        <h2>Concept</h2>
        <form class="admin-form" @submit.prevent="submitConcept">
          <label>Topic<select v-model="conceptForm.topicId" required><option value="" disabled>선택</option><option v-for="item in topics.filter(t => t.active)" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
          <label>코드<input v-model="conceptForm.code" required /></label><label>이름<input v-model="conceptForm.name" required /></label>
          <label>설명<textarea v-model="conceptForm.description" rows="3" /></label>
          <button class="admin-primary" :disabled="feedback.submitting.value">{{ conceptEditingId ? "Concept 수정" : "Concept 등록" }}</button>
        </form>
        <ul class="admin-list"><li v-for="item in concepts" :key="item.id" :class="{ inactive: !item.active }"><div><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.active ? "ACTIVE" : "INACTIVE" }}</small></div><div><button :disabled="feedback.submitting.value" @click="editConcept(item)">편집</button><button v-if="item.active" :disabled="feedback.submitting.value" @click="deactivate('concept', item.id)">비활성화</button></div></li></ul>
      </section>
    </div>
  </section>
</template>
