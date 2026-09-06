<script setup lang="ts">
import { onMounted } from "vue";
import { RouterView, useRouter } from "vue-router";

import { useAuth } from "@/composables/useAuth";
import ThemeSwitch from "@/components/ThemeSwitch.vue";

const router = useRouter();
const { currentMember, restoreAuthentication, logout } = useAuth();

onMounted(() => restoreAuthentication());

async function handleLogout() {
  await logout();
  await router.push("/login");
}
</script>

<template>
  <a class="skip-link" href="#main-content">본문으로 건너뛰기</a>
  <header class="app-header">
    <RouterLink class="brand" to="/questions">
      <span>crackCS</span>
      <span class="brand-caption">설명하며 배우는 CS</span>
    </RouterLink>
    <div class="header-tools">
    <ThemeSwitch />
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
    </div>
  </header>
  <div id="main-content" tabindex="-1"><RouterView /></div>
  <footer class="app-footer"><span>crackCS</span><span>한 질문씩, 내 언어로.</span></footer>
</template>
