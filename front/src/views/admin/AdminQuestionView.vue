<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";

import {
  createAdminQuestion,
  createQuestionVersion,
  fetchAdminQuestion,
  fetchAdminQuestions,
  fetchConcepts,
  fetchTopics,
  publishQuestion,
  replaceQuestionConcepts,
  retireQuestion,
  reviewQuestion,
  updateAdminQuestion,
  type AdminQuestion,
  type AdminQuestionInput,
  type AdminQuestionSummary,
  type Concept,
  type ContentStatus,
  type QuestionDifficulty,
  type Topic,
} from "@/api/admin";
import AdminFeedback from "@/components/AdminFeedback.vue";
import { useAdminFeedback } from "@/composables/useAdminFeedback";

const topics = ref<Topic[]>([]);
const concepts = ref<Concept[]>([]);
const questions = ref<AdminQuestionSummary[]>([]);
const selected = ref<AdminQuestion>();
let selectionGeneration = 0;
const statusFilter = ref<ContentStatus | "">("");
const loading = ref(true);
const loadError = ref(false);
const detailError = ref(false);
const feedback = useAdminFeedback();
const form = reactive({ topicId: "", difficulty: "BASIC" as QuestionDifficulty, content: "", referenceAnswer: "" });
const criteria = ref<Array<{ conceptId: number; weight: number; required: boolean }>>([]);
const newCriterionConceptId = ref<number>();
let loadGeneration = 0;
const topicConcepts = computed(() => concepts.value.filter(
  item => item.topicId === Number(form.topicId),
));
const availableConcepts = computed(() => topicConcepts.value.filter(
  item => !criteria.value.some(row => row.conceptId === item.id),
));

function input(): AdminQuestionInput {
  return { topicId: Number(form.topicId), difficulty: form.difficulty, content: form.content, referenceAnswer: form.referenceAnswer };
}

async function load() {
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = false;
  try {
    const [topicPage, conceptPage, questionPage] = await Promise.all([
      fetchTopics({ active: true, size: 100 }), fetchConcepts({ active: true, size: 100 }),
      fetchAdminQuestions({
        ...(statusFilter.value ? { status: statusFilter.value } : {}),
        size: 100,
        sort: "id,desc",
      }),
    ]);
    if (generation !== loadGeneration) return;
    topics.value = topicPage.content; concepts.value = conceptPage.content; questions.value = questionPage.content;
  } catch {
    if (generation === loadGeneration) loadError.value = true;
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}

function clearSelection() {
  selectionGeneration++;
  selected.value = undefined;
  Object.assign(form, { topicId: "", difficulty: "BASIC", content: "", referenceAnswer: "" });
  criteria.value = [];
  detailError.value = false;
}

async function select(question: AdminQuestionSummary) {
  const activeGeneration = ++selectionGeneration;
  detailError.value = false;
  try {
    const detail = await fetchAdminQuestion(question.id);
    if (activeGeneration !== selectionGeneration) return;
    selected.value = detail;
    Object.assign(form, { topicId: String(detail.topicId), difficulty: detail.difficulty, content: detail.content, referenceAnswer: detail.referenceAnswer });
    criteria.value = detail.concepts.map(item => ({ conceptId: item.conceptId, weight: item.weight, required: item.required }));
  } catch {
    if (activeGeneration === selectionGeneration) detailError.value = true;
  }
}

async function submit() {
  const activeGeneration = selectionGeneration;
  const target = selected.value ? { id: selected.value.id, status: selected.value.status } : undefined;
  const payload = input();
  const result = await feedback.execute(
    () => target?.status === "DRAFT" ? updateAdminQuestion(target.id, payload) : createAdminQuestion(payload),
    target?.status === "DRAFT" ? "문제 초안을 수정했습니다." : "문제 초안을 등록했습니다.",
  );
  if (result && activeGeneration === selectionGeneration) selected.value = result;
  if (result) await load();
}

function addCriterion() {
  const candidate = availableConcepts.value.find(item => item.id === newCriterionConceptId.value);
  if (candidate) {
    criteria.value.push({ conceptId: candidate.id, weight: 1, required: true });
    newCriterionConceptId.value = undefined;
  }
}

async function saveCriteria() {
  if (!selected.value) return;
  const activeGeneration = selectionGeneration;
  const targetId = selected.value.id;
  const payload = criteria.value.map(item => ({ ...item }));
  const result = await feedback.execute(() => replaceQuestionConcepts(targetId, payload), "평가 Concept을 교체했습니다.");
  if (result && activeGeneration === selectionGeneration) selected.value = result;
}

async function transition(action: "review" | "publish" | "retire") {
  if (!selected.value) return;
  const activeGeneration = selectionGeneration;
  const targetId = selected.value.id;
  const calls = { review: reviewQuestion, publish: publishQuestion, retire: retireQuestion };
  const messages = { review: "문제 검수를 기록했습니다.", publish: "문제를 공개했습니다.", retire: "문제를 폐기했습니다." };
  const result = await feedback.execute(() => calls[action](targetId), messages[action]);
  if (result && activeGeneration === selectionGeneration) selected.value = result;
  if (result) await load();
}

async function newVersion() {
  if (!selected.value) return;
  const activeGeneration = selectionGeneration;
  const targetId = selected.value.id;
  const payload = { difficulty: form.difficulty, content: form.content, referenceAnswer: form.referenceAnswer };
  const result = await feedback.execute(
    () => createQuestionVersion(targetId, payload),
    "문제의 새 DRAFT 버전을 만들고 평가 Concept을 복사했습니다.",
  );
  if (result && activeGeneration === selectionGeneration) {
    selected.value = result;
    criteria.value = result.concepts.map(item => ({ conceptId: item.conceptId, weight: item.weight, required: item.required }));
  }
  if (result) await load();
}

onMounted(load);
onBeforeUnmount(() => { selectionGeneration++; loadGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">QUESTION BANK</p><h1>문제</h1></div><p>모범 답안과 Concept 가중치의 합이 1.00인지 검수한 뒤 공개합니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" />
    <p v-if="loadError" class="admin-error">문제 목록을 불러오지 못했습니다. <button type="button" data-retry="list" @click="load">다시 시도</button></p>
    <p v-if="detailError" class="admin-error">문제 상세를 불러오지 못했습니다.</p>
    <div class="admin-toolbar"><label>상태 <select v-model="statusFilter" @change="load"><option value="">전체</option><option>DRAFT</option><option>PUBLISHED</option><option>RETIRED</option></select></label><button @click="clearSelection">새 문제</button></div>
    <p v-if="loading" class="admin-loading">문제를 불러오는 중…</p>
    <div v-else class="admin-editor-layout">
      <ul class="admin-list selectable"><li v-for="question in questions" :key="question.id" :class="{ selected: selected?.id === question.id }"><button type="button" :data-question-id="question.id" :aria-pressed="selected?.id === question.id" @click="select(question)"><strong>{{ question.content }}</strong><small>v{{ question.questionVersion }} · {{ question.status }} · {{ question.difficulty }}</small></button></li></ul>
      <section class="admin-panel">
        <h2>{{ selected ? `Question #${selected.id} · v${selected.questionVersion}` : "새 문제" }}</h2>
        <form class="admin-form" @submit.prevent="submit">
          <label>Topic<select v-model="form.topicId" required><option value="" disabled>선택</option><option v-for="topic in topics" :key="topic.id" :value="topic.id">{{ topic.name }}</option></select></label>
          <label>난이도<select v-model="form.difficulty"><option>BASIC</option><option>INTERMEDIATE</option><option>ADVANCED</option></select></label>
          <label>문제 본문<textarea v-model="form.content" rows="5" required /></label>
          <label>모범 답안<textarea v-model="form.referenceAnswer" rows="7" required /></label>
          <div class="admin-actions">
            <button v-if="!selected || selected.status === 'DRAFT'" class="admin-primary" :disabled="feedback.submitting.value">{{ selected ? "초안 수정" : "초안 등록" }}</button>
            <button v-if="selected?.status === 'PUBLISHED'" type="button" :disabled="feedback.submitting.value" @click="newVersion">현재 입력으로 새 버전</button>
          </div>
        </form>
        <section v-if="selected" class="criteria-panel">
          <div class="section-heading"><h3>평가 Concept</h3><div v-if="selected.status === 'DRAFT'" class="criteria-add"><select v-model.number="newCriterionConceptId" aria-label="추가할 Concept"><option :value="undefined">선택</option><option v-for="concept in availableConcepts" :key="concept.id" :value="concept.id">{{ concept.name }}</option></select><button :disabled="newCriterionConceptId === undefined" @click="addCriterion">Concept 추가</button></div></div>
          <div v-for="(row, index) in criteria" :key="`${row.conceptId}-${index}`" class="criteria-row">
            <select v-model.number="row.conceptId" :disabled="selected.status !== 'DRAFT'"><option v-for="concept in topicConcepts" :key="concept.id" :value="concept.id">{{ concept.name }}</option></select>
            <input v-model.number="row.weight" :disabled="selected.status !== 'DRAFT'" type="number" min="0.01" max="1" step="0.01" aria-label="가중치" />
            <label><input v-model="row.required" :disabled="selected.status !== 'DRAFT'" type="checkbox" /> 필수</label>
            <button v-if="selected.status === 'DRAFT'" :disabled="feedback.submitting.value" @click="criteria.splice(index, 1)">삭제</button>
          </div>
          <div class="admin-actions"><button v-if="selected.status === 'DRAFT'" :disabled="feedback.submitting.value" @click="saveCriteria">평가 기준 저장</button><button v-if="selected.status === 'DRAFT'" :disabled="feedback.submitting.value" @click="transition('review')">검수</button><button v-if="selected.status === 'DRAFT'" class="admin-primary" :disabled="feedback.submitting.value" @click="transition('publish')">공개</button><button v-if="selected.status === 'PUBLISHED'" :disabled="feedback.submitting.value" @click="transition('retire')">폐기</button></div>
        </section>
      </section>
    </div>
  </section>
</template>
