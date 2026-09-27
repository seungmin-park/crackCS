<script setup lang="ts">
import RequestFailure from "@/components/RequestFailure.vue";
import { presentRequestError, type RequestErrorPresentation } from "@/presentation/requestErrorPresentation";

import {
  onBeforeUnmount,
  onMounted,
  ref,
  watch
} from "vue";
import {
  useRoute,
  useRouter
} from "vue-router";

import {
  fetchKnowledgeDocuments,
  type KnowledgeDocument
} from "@/api/admin/knowledgeDocuments";
import {
  fetchTopics,
  type Topic
} from "@/api/admin/topics";
import type {
  ContentStatus
} from "@/api/admin/types";
import AdminFeedback from "@/components/AdminFeedback.vue";
import AdminPagination from "@/components/AdminPagination.vue";
import {
  useKnowledgeDocumentEditor
} from "@/composables/useKnowledgeDocumentEditor";
import {
  ADMIN_PAGE_SIZE,
  fetchAllPages,
  normalizedPage,
  queryPage,
  queryStringValue,
  replaceAdminQuery,
  updateAdminQuery
} from "./adminPagination";

const route = useRoute();
const router = useRouter();
const topics = ref<Topic[]>([]);
const documents = ref<KnowledgeDocument[]>([]);
const loading = ref(true);
const loadError = ref<RequestErrorPresentation>();
const statusFilter = ref<ContentStatus | "">(queryStringValue(route.query,
  "status") as ContentStatus | "");
let routeStatus = statusFilter.value;
const page = ref(queryPage(route.query));
const totalPages = ref(0);
const totalElements = ref(0);
const relationLoading = ref(true);
const relationError = ref<RequestErrorPresentation>();
let listGeneration = 0;
let relationGeneration = 0;
let disposed = false;

async function loadKnowledgeDocuments() {
  if (disposed) return;
  const generation = ++listGeneration;
  loading.value = true;
  loadError.value = undefined;
  try {
    const documentPage = await fetchKnowledgeDocuments({
      ...(statusFilter.value ? {
        status: statusFilter.value
      } : {}),
      page: page.value,
      size: ADMIN_PAGE_SIZE,
      sort: "id,desc",
    });
    if (generation !== listGeneration) return;
    const validPage = normalizedPage(page.value, documentPage.totalPages);
    if (validPage !== page.value) {
      await replaceAdminQuery(router, route.query, {
        page: validPage
      });
      return;
    }
    documents.value = documentPage.content;
    page.value = documentPage.page;
    totalPages.value = documentPage.totalPages;
    totalElements.value = documentPage.totalElements;
  } catch (caught) {
    if (generation === listGeneration) loadError.value = presentRequestError(caught);
  } finally {
    if (generation === listGeneration) loading.value = false;
  }
}

async function loadRelations() {
  if (disposed) return;
  const generation = ++relationGeneration;
  relationLoading.value = true;
  relationError.value = undefined;
  try {
    const activeTopics = await fetchAllPages((candidatePage, size) => fetchTopics({
      active: true,
      page: candidatePage,
      size
    }));
    if (generation === relationGeneration) topics.value = activeTopics;
  } catch (caught) {
    if (generation === relationGeneration) relationError.value = presentRequestError(caught);
  } finally {
    if (generation === relationGeneration) relationLoading.value = false;
  }
}

async function changeFilter() {
  selectDocumentForEditing();
  await updateAdminQuery(router, route.query, {
    page: 0,
    status: statusFilter.value || undefined
  });
}

async function changePage(nextPage: number) {
  await updateAdminQuery(router, route.query, {
    page: nextPage,
    status: statusFilter.value || undefined
  });
}

watch(() => route.query, () => {
  const nextStatus = queryStringValue(route.query, "status") as ContentStatus | "";
  if (nextStatus !== routeStatus) selectDocumentForEditing();
  routeStatus = nextStatus;
  statusFilter.value = nextStatus;
  page.value = queryPage(route.query);
  void loadKnowledgeDocuments();
}, {
  deep: true
});

const {
  selectedDocument,
  feedback,
  chunks,
  chunkError,
  chunksLoading,
  form,
  selectDocumentForEditing,
  loadChunks,
  generateDocumentChunks,
  saveDocumentDraft,
  createVersion,
  reviewSelectedDocument,
  publishSelectedDocument,
  retireSelectedDocument,
} = useKnowledgeDocumentEditor(loadKnowledgeDocuments);

onMounted(() => {
  void loadKnowledgeDocuments();
  void loadRelations();
});
onBeforeUnmount(() => {
  disposed = true;
  listGeneration++;
  relationGeneration++;
});
</script>

<template>
  <section>
    <header class="admin-page-heading">
      <div>
        <p class="eyebrow">KNOWLEDGE</p>
        <h1>근거 문서</h1>
      </div>
      <p>공개본은 수정하지 않고 같은 계열의 새 버전을 만듭니다.</p>
    </header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" :field-errors="feedback.fieldErrors.value" />
    <RequestFailure v-if="relationError" :failure="relationError" title="관계 후보를 불러오지 못했습니다." retry-key="relations" @retry="loadRelations" />
    <p v-else-if="relationLoading" role="status" aria-busy="true" class="admin-loading">관계 후보를 불러오는 중…</p>
    <RequestFailure v-if="loadError" :failure="loadError" title="문서 목록을 불러오지 못했습니다." retry-key="list" @retry="loadKnowledgeDocuments" />
    <div class="admin-toolbar"><label>상태 <select v-model="statusFilter" @change="changeFilter">
          <option value="">전체</option>
          <option>DRAFT</option>
          <option>PUBLISHED</option>
          <option>RETIRED</option>
        </select></label><button @click="selectDocumentForEditing()">새 문서</button></div>
    <p v-if="loading" role="status" aria-busy="true" class="admin-loading">문서를 불러오는 중…</p>
    <div v-else-if="!loadError" class="admin-editor-layout">
      <p v-if="!documents.length" role="status">조건에 맞는 문서가 없습니다.</p>
      <ul v-else class="admin-list selectable">
        <li v-for="document in documents" :key="document.id"
          :class="{ selected: selectedDocument?.id === document.id }"><button type="button"
            :data-document-id="document.id" :aria-pressed="selectedDocument?.id === document.id"
            @click="selectDocumentForEditing(document)"><strong>{{ document.title }}</strong><small>v{{ document.documentVersion }}
              · {{ document.status }} ·
              {{ document.technologyVersion || "버전 미입력" }}</small></button></li>
      </ul>
      <section class="admin-panel">
        <h2>{{ selectedDocument ? `${selectedDocument.title} · v${selectedDocument.documentVersion}` : "새 문서" }}</h2>
        <form class="admin-form" @submit.prevent="saveDocumentDraft">
          <label>Topic<select v-model="form.topicId" required>
              <option value="" disabled>선택</option>
              <option v-for="topic in topics" :key="topic.id" :value="topic.id">{{ topic.name }}
              </option>
            </select></label>
          <label>제목<input v-model="form.title"
              required /><small>{{ feedback.fieldErrors.value.title }}</small></label>
          <label>출처 유형<select v-model="form.sourceType">
              <option>OFFICIAL_SPEC</option>
              <option>OFFICIAL_DOC</option>
              <option>INTERNAL_SUMMARY</option>
            </select></label>
          <label>출처 URL<input v-model="form.sourceUrl"
              type="url" /><small>{{ feedback.fieldErrors.value.sourceUrl }}</small></label>
          <label>기술 버전<input v-model="form.technologyVersion" placeholder="Java 21" /></label>
          <label>라이선스 메모<textarea v-model="form.licenseNote" rows="2" /></label>
          <label>원문<textarea v-model="form.content" rows="10"
              required /><small>{{ feedback.fieldErrors.value.content }}</small></label>
          <div class="admin-actions">
            <button v-if="!selectedDocument || selectedDocument.status === 'DRAFT'" class="admin-primary"
              :disabled="feedback.submitting.value">{{ selectedDocument ? "초안 수정" : "초안 등록" }}</button>
            <button v-if="selectedDocument?.status === 'PUBLISHED'" type="button"
              :disabled="feedback.submitting.value" @click="createVersion">현재 입력으로 새 버전</button>
            <button v-if="selectedDocument?.status === 'DRAFT'" type="button"
              :disabled="feedback.submitting.value" @click="reviewSelectedDocument">검수</button>
            <button v-if="selectedDocument?.status === 'DRAFT'" type="button"
              :disabled="feedback.submitting.value" @click="publishSelectedDocument">공개</button>
            <button v-if="selectedDocument?.status === 'PUBLISHED'" type="button"
              :disabled="feedback.submitting.value" @click="retireSelectedDocument">폐기</button>
            <button v-if="selectedDocument?.status === 'PUBLISHED'" type="button"
              :disabled="feedback.submitting.value" @click="generateDocumentChunks">검색 문단 생성</button>
          </div>
        </form>
        <p v-if="selectedDocument" class="admin-meta">checksum {{ selectedDocument.checksum }}<br />series
          {{ selectedDocument.versionSeriesId }}</p>
        <section v-if="selectedDocument?.status === 'PUBLISHED'" class="admin-chunks">
          <h3>검색 문단 · {{ chunks.length }}개</h3>
          <p v-if="chunksLoading" role="status" aria-busy="true" class="admin-loading">검색 문단을 불러오는 중…</p>
          <RequestFailure v-if="chunkError" :failure="chunkError" title="검색 문단을 불러오지 못했습니다." @retry="loadChunks(selectedDocument.id)" />
          <p v-if="!chunksLoading && !chunkError && !chunks.length">아직 생성된 검색 문단이 없습니다.</p>
          <article v-for="chunk in (!chunkError && !chunksLoading ? chunks : [])" :key="chunk.id">
            <strong>#{{ chunk.sequenceNo }} · {{ chunk.searchStatus }} ·
              {{ chunk.startOffset }}–{{ chunk.endOffset }}</strong>
            <p>{{ chunk.content }}</p>
          </article>
        </section>
      </section>
    </div>
    <AdminPagination v-if="!loading && !loadError" :page="page" :total-pages="totalPages" :total-elements="totalElements"
      @change="changePage" />
  </section>
</template>
