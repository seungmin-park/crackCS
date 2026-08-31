<script setup lang="ts">
import { onMounted } from "vue";
import { RouterView, useRouter } from "vue-router";

import { useAuth } from "@/composables/useAuth";

const router = useRouter();
const { currentMember, restoreAuthentication, logout } = useAuth();

onMounted(() => restoreAuthentication());

async function handleLogout() {
  await logout();
  await router.push("/login");
}
</script>

<template>
  <header class="app-header">
    <RouterLink class="brand" to="/questions">
      <span class="brand-mark" aria-hidden="true">C</span>
      <span>crackCS</span>
    </RouterLink>
    <nav class="header-actions" aria-label="계정 메뉴">
      <template v-if="currentMember">
        <span class="member-caption">{{ currentMember.nickname }}</span>
        <RouterLink v-if="currentMember.role === 'ADMIN'" to="/admin">관리</RouterLink>
        <button class="header-text-button" type="button" @click="handleLogout">로그아웃</button>
      </template>
      <template v-else>
        <RouterLink class="header-login" to="/login">로그인</RouterLink>
        <RouterLink class="header-join" to="/sign-up">시작하기</RouterLink>
      </template>
    </nav>
  </header>
  <RouterView />
</template>
