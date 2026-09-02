<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";

import {
  createKnowledgeDocument,
  createKnowledgeDocumentVersion,
  fetchKnowledgeDocuments,
  fetchTopics,
  publishKnowledgeDocument,
  retireKnowledgeDocument,
  reviewKnowledgeDocument,
  updateKnowledgeDocument,
  type ContentStatus,
  type KnowledgeDocument,
  type KnowledgeDocumentInput,
  type KnowledgeSourceType,
  type Topic,
} from "@/api/admin";
import AdminFeedback from "@/components/AdminFeedback.vue";
import { useAdminFeedback } from "@/composables/useAdminFeedback";

const topics = ref<Topic[]>([]);
const documents = ref<KnowledgeDocument[]>([]);
const loading = ref(true);
const statusFilter = ref<ContentStatus | "">("");
const selected = ref<KnowledgeDocument>();
const feedback = useAdminFeedback();
const form = reactive({ topicId: "", title: "", sourceType: "OFFICIAL_DOC" as KnowledgeSourceType, sourceUrl: "", technologyVersion: "", licenseNote: "", content: "" });

function input(): KnowledgeDocumentInput {
  return { topicId: Number(form.topicId), title: form.title, sourceType: form.sourceType, sourceUrl: form.sourceUrl, technologyVersion: form.technologyVersion, licenseNote: form.licenseNote, content: form.content };
}

async function load() {
  loading.value = true;
  const [topicPage, documentPage] = await Promise.all([
    fetchTopics({ active: true, size: 100 }),
    fetchKnowledgeDocuments({
      ...(statusFilter.value ? { status: statusFilter.value } : {}),
      size: 100,
      sort: "id,desc",
    }),
  ]);
  topics.value = topicPage.content;
  documents.value = documentPage.content;
  loading.value = false;
}

function select(document?: KnowledgeDocument) {
  selected.value = document;
  Object.assign(form, document ? {
    topicId: String(document.topicId), title: document.title, sourceType: document.sourceType,
    sourceUrl: document.sourceUrl ?? "", technologyVersion: document.technologyVersion ?? "",
    licenseNote: document.licenseNote ?? "", content: document.content,
  } : { topicId: "", title: "", sourceType: "OFFICIAL_DOC", sourceUrl: "", technologyVersion: "", licenseNote: "", content: "" });
}

async function submit() {
  const result = await feedback.execute(
    () => selected.value?.status === "DRAFT" ? updateKnowledgeDocument(selected.value.id, input()) : createKnowledgeDocument(input()),
    selected.value?.status === "DRAFT" ? "문서 초안을 수정했습니다. 수정 후에는 다시 검수해야 합니다." : "문서 초안을 등록했습니다.",
  );
  if (result) { select(result); await load(); }
}

async function createVersion() {
  if (!selected.value) return;
  const result = await feedback.execute(() => createKnowledgeDocumentVersion(selected.value!.id, input()), "새 DRAFT 버전을 생성했습니다.");
  if (result) { select(result); await load(); }
}

async function transition(action: "review" | "publish" | "retire") {
  if (!selected.value) return;
  const calls = { review: reviewKnowledgeDocument, publish: publishKnowledgeDocument, retire: retireKnowledgeDocument };
  const messages = { review: "문서 검수를 기록했습니다.", publish: "문서를 공개했습니다.", retire: "문서를 폐기 상태로 전환했습니다." };
  const result = await feedback.execute(() => calls[action](selected.value!.id), messages[action]);
  if (result) { select(result); await load(); }
}

onMounted(load);
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">KNOWLEDGE</p><h1>Knowledge Document</h1></div><p>공개본은 수정하지 않고 같은 계열의 새 버전을 만듭니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" />
    <div class="admin-toolbar"><label>상태 <select v-model="statusFilter" @change="load"><option value="">전체</option><option>DRAFT</option><option>PUBLISHED</option><option>RETIRED</option></select></label><button @click="select()">새 문서</button></div>
    <p v-if="loading" class="admin-loading">문서를 불러오는 중…</p>
    <div v-else class="admin-editor-layout">
      <ul class="admin-list selectable"><li v-for="document in documents" :key="document.id" :class="{ selected: selected?.id === document.id }" @click="select(document)"><div><strong>{{ document.title }}</strong><small>v{{ document.documentVersion }} · {{ document.status }} · {{ document.technologyVersion || "버전 미입력" }}</small></div></li></ul>
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
            <button v-if="selected?.status === 'PUBLISHED'" type="button" @click="createVersion">현재 입력으로 새 버전</button>
            <button v-if="selected?.status === 'DRAFT'" type="button" @click="transition('review')">검수</button>
            <button v-if="selected?.status === 'DRAFT'" type="button" @click="transition('publish')">공개</button>
            <button v-if="selected?.status === 'PUBLISHED'" type="button" @click="transition('retire')">폐기</button>
          </div>
        </form>
        <p v-if="selected" class="admin-meta">checksum {{ selected.checksum }}<br />series {{ selected.versionSeriesId }}</p>
      </section>
    </div>
  </section>
</template>
