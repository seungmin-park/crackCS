<script setup lang="ts">
import { onMounted, ref } from "vue";

import { fetchAdminMembers, updateMemberStatus, type AdminMember, type MemberStatus } from "@/api/admin";
import AdminFeedback from "@/components/AdminFeedback.vue";
import { useAdminFeedback } from "@/composables/useAdminFeedback";

const members = ref<AdminMember[]>([]);
const feedback = useAdminFeedback();

async function load() { members.value = (await fetchAdminMembers({ size: 100 })).content; }
async function change(member: AdminMember, status: MemberStatus) {
  const result = await feedback.execute(() => updateMemberStatus(member.id, status), "회원 상태를 변경했습니다.");
  if (result) await load();
}
onMounted(load);
</script>

<template>
  <section>
    <header class="admin-page-heading"><div><p class="eyebrow">MEMBERS</p><h1>회원</h1></div><p>BLOCKED와 WITHDRAWN 회원은 다음 인증부터 로그인할 수 없습니다.</p></header>
    <AdminFeedback :success="feedback.successMessage.value" :error="feedback.formError.value" />
    <ul class="admin-list"><li v-for="member in members" :key="member.id"><div><strong>{{ member.nickname }}</strong><small>#{{ member.id }} · {{ member.role }} · {{ member.status }}</small></div><select :value="member.status" @change="change(member, ($event.target as HTMLSelectElement).value as MemberStatus)"><option>ACTIVE</option><option>BLOCKED</option><option>WITHDRAWN</option></select></li></ul>
  </section>
</template>
