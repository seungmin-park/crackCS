<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";

import {
  createKnowledgeDocument,
  createKnowledgeDocumentVersion,
  fetchKnowledgeDocuments,
  publishKnowledgeDocument,
  retireKnowledgeDocument,
  reviewKnowledgeDocument,
  updateKnowledgeDocument,
  fetchKnowledgeChunks,
  generateKnowledgeChunks,
  type KnowledgeDocument,
  type KnowledgeDocumentInput,
  type KnowledgeSourceType,
  type KnowledgeChunk,
} from "@/api/admin/knowledgeDocuments";
import { fetchTopics, type Topic } from "@/api/admin/topics";
import type { ContentStatus } from "@/api/admin/types";
import AdminFeedback from "@/components/AdminFeedback.vue";
import { useAdminFeedback } from "@/composables/useAdminFeedback";

const topics = ref<Topic[]>([]);
const documents = ref<KnowledgeDocument[]>([]);
const loading = ref(true);
const loadError = ref(false);
const statusFilter = ref<ContentStatus | "">("");
const selected = ref<KnowledgeDocument>();
const feedback = useAdminFeedback();
const chunks = ref<KnowledgeChunk[]>([]);
const chunkError = ref(false);
const chunksLoading = ref(false);
let listGeneration = 0;
let selectionGeneration = 0;
let chunkRequestGeneration = 0;
const form = reactive({ topicId: "", title: "", sourceType: "OFFICIAL_DOC" as KnowledgeSourceType, sourceUrl: "", technologyVersion: "", licenseNote: "", content: "" });

function input(): KnowledgeDocumentInput {
  return { topicId: Number(form.topicId), title: form.title, sourceType: form.sourceType, sourceUrl: form.sourceUrl, technologyVersion: form.technologyVersion, licenseNote: form.licenseNote, content: form.content };
}

async function load() {
  const generation = ++listGeneration;
  loading.value = true;
  loadError.value = false;
  try {
    const [topicPage, documentPage] = await Promise.all([
      fetchTopics({ active: true, size: 100 }),
      fetchKnowledgeDocuments({
        ...(statusFilter.value ? { status: statusFilter.value } : {}), size: 100, sort: "id,desc",
      }),
    ]);
    if (generation !== listGeneration) return;
    topics.value = topicPage.content;
    documents.value = documentPage.content;
  } catch {
    if (generation === listGeneration) loadError.value = true;
  } finally {
    if (generation === listGeneration) loading.value = false;
  }
}

function changeFilter() {
  select();
  void load();
}

function select(document?: KnowledgeDocument) {
  selectionGeneration++;
  chunkRequestGeneration++;
  selected.value = document;
  Object.assign(form, document ? {
    topicId: String(document.topicId), title: document.title, sourceType: document.sourceType,
    sourceUrl: document.sourceUrl ?? "", technologyVersion: document.technologyVersion ?? "",
    licenseNote: document.licenseNote ?? "", content: document.content,
  } : { topicId: "", title: "", sourceType: "OFFICIAL_DOC", sourceUrl: "", technologyVersion: "", licenseNote: "", content: "" });
  chunks.value = [];
  chunkError.value = false;
  chunksLoading.value = false;
  if (document) void loadChunks(document.id);
}

async function loadChunks(documentId: number) {
  const selection = selectionGeneration;
  const request = ++chunkRequestGeneration;
  chunksLoading.value = true;
  chunkError.value = false;
  try {
    const result = await fetchKnowledgeChunks(documentId);
    if (selection === selectionGeneration && request === chunkRequestGeneration && selected.value?.id === documentId) chunks.value = result;
  } catch {
    if (selection === selectionGeneration && request === chunkRequestGeneration && selected.value?.id === documentId) chunkError.value = true;
  } finally {
    if (selection === selectionGeneration && request === chunkRequestGeneration && selected.value?.id === documentId) chunksLoading.value = false;
  }
}

async function chunkDocument() {
  if (!selected.value) return;
  const generation = selectionGeneration;
  const targetId = selected.value.id;
  chunkRequestGeneration++;
  chunksLoading.value = false;
  chunkError.value = false;
  const result = await feedback.execute(
    () => generateKnowledgeChunks(targetId),
    "검색용 문단을 생성했습니다. 같은 문서와 정책이면 기존 결과를 재사용합니다.",
  );
  if (result && generation === selectionGeneration && selected.value?.id === targetId) {
    chunkRequestGeneration++;
    chunks.value = result.chunks;
    chunkError.value = false;
    chunksLoading.value = false;
  }
}

async function submit() {
  const generation = selectionGeneration;
  const target = selected.value ? { id: selected.value.id, status: selected.value.status } : undefined;
  const payload = input();
  const result = await feedback.execute(
    () => target?.status === "DRAFT" ? updateKnowledgeDocument(target.id, payload) : createKnowledgeDocument(payload),
    target?.status === "DRAFT" ? "문서 초안을 수정했습니다. 수정 후에는 다시 검수해야 합니다." : "문서 초안을 등록했습니다.",
  );
  if (result && generation === selectionGeneration) select(result);
  if (result) await load();
}

async function createVersion() {
  if (!selected.value) return;
  const generation = selectionGeneration;
  const targetId = selected.value.id;
  const payload = input();
  const result = await feedback.execute(() => createKnowledgeDocumentVersion(targetId, payload), "새 DRAFT 버전을 생성했습니다.");
  if (result && generation === selectionGeneration) select(result);
  if (result) await load();
}

async function transition(action: "review" | "publish" | "retire") {
  if (!selected.value) return;
  const generation = selectionGeneration;
  const targetId = selected.value.id;
  const calls = { review: reviewKnowledgeDocument, publish: publishKnowledgeDocument, retire: retireKnowledgeDocument };
  const messages = { review: "문서 검수를 기록했습니다.", publish: "문서를 공개했습니다.", retire: "문서를 폐기 상태로 전환했습니다." };
  const result = await feedback.execute(() => calls[action](targetId), messages[action]);
  if (result && generation === selectionGeneration) select(result);
  if (result) await load();
}

onMounted(load);
onBeforeUnmount(() => { listGeneration++; selectionGeneration++; chunkRequestGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">KNOWLEDGE</p><h1>근거 문서</h1></div><p>공개본은 수정하지 않고 같은 계열의 새 버전을 만듭니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" />
    <p v-if="loadError" class="admin-error">문서 목록을 불러오지 못했습니다. <button type="button" data-retry="list" @click="load">다시 시도</button></p>
    <div class="admin-toolbar"><label>상태 <select v-model="statusFilter" @change="changeFilter"><option value="">전체</option><option>DRAFT</option><option>PUBLISHED</option><option>RETIRED</option></select></label><button @click="select()">새 문서</button></div>
    <p v-if="loading" class="admin-loading">문서를 불러오는 중…</p>
    <div v-else class="admin-editor-layout">
      <ul class="admin-list selectable"><li v-for="document in documents" :key="document.id" :class="{ selected: selected?.id === document.id }"><button type="button" :data-document-id="document.id" :aria-pressed="selected?.id === document.id" @click="select(document)"><strong>{{ document.title }}</strong><small>v{{ document.documentVersion }} · {{ document.status }} · {{ document.technologyVersion || "버전 미입력" }}</small></button></li></ul>
      <section class="admin-panel">
        <h2>{{ selected ? `${selected.title} · v${selected.documentVersion}` : "새 문서" }}</h2>
        <form class="admin-form" @submit.prevent="submit">
          <label>Topic<select v-model="form.topicId" required><option value="" disabled>선택</option><option v-for="topic in topics" :key="topic.id" :value="topic.id">{{ topic.name }}</option></select></label>
          <label>제목<input v-model="form.title" required /><small>{{ feedback.fieldErrors.value.title }}</small></label>
          <label>출처 유형<select v-model="form.sourceType"><option>OFFICIAL_SPEC</option><option>OFFICIAL_DOC</option><option>INTERNAL_SUMMARY</option></select></label>
          <label>출처 URL<input v-model="form.sourceUrl" type="url" /><small>{{ feedback.fieldErrors.value.sourceUrl }}</small></label>
          <label>기술 버전<input v-model="form.technologyVersion" placeholder="Java 21" /></label>
          <label>라이선스 메모<textarea v-model="form.licenseNote" rows="2" /></label>
          <label>원문<textarea v-model="form.content" rows="10" required /><small>{{ feedback.fieldErrors.value.content }}</small></label>
          <div class="admin-actions">
            <button v-if="!selected || selected.status === 'DRAFT'" class="admin-primary" :disabled="feedback.submitting.value">{{ selected ? "초안 수정" : "초안 등록" }}</button>
            <button v-if="selected?.status === 'PUBLISHED'" type="button" :disabled="feedback.submitting.value" @click="createVersion">현재 입력으로 새 버전</button>
            <button v-if="selected?.status === 'DRAFT'" type="button" :disabled="feedback.submitting.value" @click="transition('review')">검수</button>
            <button v-if="selected?.status === 'DRAFT'" type="button" :disabled="feedback.submitting.value" @click="transition('publish')">공개</button>
            <button v-if="selected?.status === 'PUBLISHED'" type="button" :disabled="feedback.submitting.value" @click="transition('retire')">폐기</button>
            <button v-if="selected?.status === 'PUBLISHED'" type="button" :disabled="feedback.submitting.value" @click="chunkDocument">검색 문단 생성</button>
          </div>
        </form>
        <p v-if="selected" class="admin-meta">checksum {{ selected.checksum }}<br />series {{ selected.versionSeriesId }}</p>
        <section v-if="selected?.status === 'PUBLISHED'" class="admin-chunks">
          <h3>검색 문단 · {{ chunks.length }}개</h3>
          <p v-if="chunksLoading" class="admin-loading">검색 문단을 불러오는 중…</p>
          <p v-if="chunkError" class="admin-error">검색 문단을 불러오지 못했습니다. <button type="button" @click="loadChunks(selected.id)">다시 시도</button></p>
          <p v-if="!chunksLoading && !chunkError && !chunks.length">아직 생성된 검색 문단이 없습니다.</p>
          <article v-for="chunk in chunks" :key="chunk.id">
            <strong>#{{ chunk.sequenceNo }} · {{ chunk.searchStatus }} · {{ chunk.startOffset }}–{{ chunk.endOffset }}</strong>
            <p>{{ chunk.content }}</p>
          </article>
        </section>
      </section>
    </div>
  </section>
</template>
