<script setup lang="ts">
import RequestFailure from "@/components/RequestFailure.vue";
import { presentRequestError, type RequestErrorPresentation } from "@/presentation/requestErrorPresentation";

import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import { fetchAdminMembers, updateMemberStatus, type AdminMember, type MemberStatus } from "@/api/admin/members";
import AdminFeedback from "@/components/AdminFeedback.vue";
import AdminPagination from "@/components/AdminPagination.vue";
import { useAdminFeedback } from "@/composables/useAdminFeedback";
import { ADMIN_PAGE_SIZE, normalizedPage, queryPage, queryStringValue, replaceAdminQuery, updateAdminQuery } from "./adminPagination";

const route = useRoute();
const router = useRouter();
const members = ref<AdminMember[]>([]);
const statusFilter = ref<MemberStatus | "">(queryStringValue(route.query, "status") as MemberStatus | "");
const page = ref(queryPage(route.query));
const totalPages = ref(0);
const totalElements = ref(0);
const feedback = useAdminFeedback();
const loading = ref(true);
const loadError = ref<RequestErrorPresentation>();
let loadGeneration = 0;
let disposed = false;

async function loadMembers() {
  if (disposed) return;
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = undefined;
  try {
    const memberPage = await fetchAdminMembers({ ...(statusFilter.value ? { status: statusFilter.value } : {}), page: page.value, size: ADMIN_PAGE_SIZE });
    if (generation === loadGeneration) {
      const validPage = normalizedPage(page.value, memberPage.totalPages);
      if (validPage !== page.value) {
        await replaceAdminQuery(router, route.query, { page: validPage });
        return;
      }
      members.value = memberPage.content; page.value = memberPage.page;
      totalPages.value = memberPage.totalPages; totalElements.value = memberPage.totalElements;
    }
  } catch (caught) {
    if (generation === loadGeneration) loadError.value = presentRequestError(caught);
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}
async function changeFilter() {
  await updateAdminQuery(router, route.query, { page: 0, status: statusFilter.value || undefined });
}
async function changePage(nextPage: number) {
  await updateAdminQuery(router, route.query, { page: nextPage, status: statusFilter.value || undefined });
}
watch(() => route.query, () => {
  statusFilter.value = queryStringValue(route.query, "status") as MemberStatus | "";
  page.value = queryPage(route.query);
  void loadMembers();
}, { deep: true });
async function changeMemberStatus(member: AdminMember, status: MemberStatus) {
  const targetId = member.id;
  const targetStatus = status;
  const updatedMember = await feedback.execute(() => updateMemberStatus(targetId, targetStatus), "회원 상태를 변경했습니다.");
  if (updatedMember && !disposed) await loadMembers();
}
onMounted(loadMembers);
onBeforeUnmount(() => { disposed = true; loadGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">MEMBERS</p><h1>회원</h1></div><p>차단·탈퇴한 회원은 기존 세션도 다음 요청부터 사용할 수 없습니다. 다시 활성화하면 재로그인이 필요합니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" :field-errors="feedback.fieldErrors.value" />
    <div class="admin-toolbar"><label>상태 <select v-model="statusFilter" @change="changeFilter"><option value="">전체</option><option>ACTIVE</option><option>BLOCKED</option><option>WITHDRAWN</option></select></label></div>
    <RequestFailure v-if="loadError" :failure="loadError" title="회원 목록을 불러오지 못했습니다." retry-key="list" @retry="loadMembers" />
    <p v-if="loading" role="status" aria-busy="true" class="admin-loading">회원을 불러오는 중…</p>
    <p v-else-if="!loadError && !members.length" role="status">조건에 맞는 회원이 없습니다.</p>
    <ul v-else-if="!loadError" class="admin-list"><li v-for="member in members" :key="member.id"><div><strong>{{ member.nickname }}</strong><small>#{{ member.id }} · {{ member.role }} · {{ member.status }}</small></div><select :value="member.status" :disabled="feedback.submitting.value" @change="changeMemberStatus(member, ($event.target as HTMLSelectElement).value as MemberStatus)"><option>ACTIVE</option><option>BLOCKED</option><option>WITHDRAWN</option></select></li></ul>
    <AdminPagination v-if="!loading && !loadError" :page="page" :total-pages="totalPages" :total-elements="totalElements" @change="changePage" />
  </section>
</template>
