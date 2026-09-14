<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";

import { fetchAdminMembers, updateMemberStatus, type AdminMember, type MemberStatus } from "@/api/admin/members";
import AdminFeedback from "@/components/AdminFeedback.vue";
import { useAdminFeedback } from "@/composables/useAdminFeedback";

const members = ref<AdminMember[]>([]);
const feedback = useAdminFeedback();
const loading = ref(true);
const loadError = ref(false);
let loadGeneration = 0;

async function load() {
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = false;
  try {
    const result = await fetchAdminMembers({ size: 100 });
    if (generation === loadGeneration) members.value = result.content;
  } catch {
    if (generation === loadGeneration) loadError.value = true;
  } finally {
    if (generation === loadGeneration) loading.value = false;
  }
}
async function change(member: AdminMember, status: MemberStatus) {
  const targetId = member.id;
  const targetStatus = status;
  const result = await feedback.execute(() => updateMemberStatus(targetId, targetStatus), "회원 상태를 변경했습니다.");
  if (result) await load();
}
onMounted(load);
onBeforeUnmount(() => { loadGeneration++; });
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">MEMBERS</p><h1>회원</h1></div><p>BLOCKED와 WITHDRAWN 회원은 다음 인증부터 로그인할 수 없습니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" />
    <p v-if="loadError" class="admin-error">회원 목록을 불러오지 못했습니다. <button type="button" data-retry="list" @click="load">다시 시도</button></p>
    <p v-if="loading" class="admin-loading">회원을 불러오는 중…</p>
    <ul v-else class="admin-list"><li v-for="member in members" :key="member.id"><div><strong>{{ member.nickname }}</strong><small>#{{ member.id }} · {{ member.role }} · {{ member.status }}</small></div><select :value="member.status" :disabled="feedback.submitting.value" @change="change(member, ($event.target as HTMLSelectElement).value as MemberStatus)"><option>ACTIVE</option><option>BLOCKED</option><option>WITHDRAWN</option></select></li></ul>
  </section>
</template>
