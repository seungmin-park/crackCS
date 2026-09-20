<script setup lang="ts">
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
const loadError = ref(false);
let loadGeneration = 0;
let disposed = false;

async function load() {
  if (disposed) return;
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = false;
  try {
    const result = await fetchAdminMembers({ ...(statusFilter.value ? { status: statusFilter.value } : {}), page: page.value, size: ADMIN_PAGE_SIZE });
    if (generation === loadGeneration) {
      const validPage = normalizedPage(page.value, result.totalPages);
      if (validPage !== page.value) {
        await replaceAdminQuery(router, route.query, { page: validPage });
        return;
      }
      members.value = result.content; page.value = result.page;
      totalPages.value = result.totalPages; totalElements.value = result.totalElements;
    }
  } catch {
    if (generation === loadGeneration) loadError.value = true;
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
  void load();
}, { deep: true });
async function change(member: AdminMember, status: MemberStatus) {
  const targetId = member.id;
  const targetStatus = status;
  const result = await feedback.execute(() => updateMemberStatus(targetId, targetStatus), "회원 상태를 변경했습니다.");
  if (result && !disposed) await load();
}
onMounted(load);
onBeforeUnmount(() => { disposed = true; loadGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">MEMBERS</p><h1>회원</h1></div><p>BLOCKED와 WITHDRAWN 회원은 다음 인증부터 로그인할 수 없습니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" />
    <div class="admin-toolbar"><label>상태 <select v-model="statusFilter" @change="changeFilter"><option value="">전체</option><option>ACTIVE</option><option>BLOCKED</option><option>WITHDRAWN</option></select></label></div>
    <p v-if="loadError" class="admin-error">회원 목록을 불러오지 못했습니다. <button type="button" data-retry="list" @click="load">다시 시도</button></p>
    <p v-if="loading" class="admin-loading">회원을 불러오는 중…</p>
    <ul v-else class="admin-list"><li v-for="member in members" :key="member.id"><div><strong>{{ member.nickname }}</strong><small>#{{ member.id }} · {{ member.role }} · {{ member.status }}</small></div><select :value="member.status" :disabled="feedback.submitting.value" @change="change(member, ($event.target as HTMLSelectElement).value as MemberStatus)"><option>ACTIVE</option><option>BLOCKED</option><option>WITHDRAWN</option></select></li></ul>
    <AdminPagination :page="page" :total-pages="totalPages" :total-elements="totalElements" @change="changePage" />
  </section>
</template>
