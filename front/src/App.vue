<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterView, useRouter } from "vue-router";

import { useAuth } from "@/composables/useAuth";
import ThemeSwitch from "@/components/ThemeSwitch.vue";

const router = useRouter();
const { currentMember, logout } = useAuth();
const logoutError = ref("");
const loggingOut = ref(false);
const menuOpen = ref(false);
const menuToggle = ref<HTMLButtonElement>();
const navigation = ref<HTMLElement>();
const learnerWorkspace = computed(() => currentMember.value?.role === "USER");
const pageTitle = computed(() => {
  const path = router.currentRoute?.value.path ?? "/";
  if (path.startsWith("/questions/")) return "답변 작성";
  if (path.startsWith("/answers/")) return "평가 결과";
  return (
    (
      {
        "/": "오늘의 학습",
        "/questions": "문제집",
        "/knowledge-map": "지식 지도",
        "/answers": "답변 이력",
        "/about": "서비스 소개",
      } as Record<string, string>
    )[path] ?? "CrackCS"
  );
});
let previousOverflow = "";
let mobileNavigationMedia: MediaQueryList | undefined;
function closeDesktopNavigation() {
  if (!mobileNavigationMedia?.matches) menuOpen.value = false;
}
onMounted(() => {
  if (typeof matchMedia !== "function") return;
  mobileNavigationMedia = matchMedia("(max-width: 760px)");
  mobileNavigationMedia.addEventListener("change", closeDesktopNavigation);
});
watch(menuOpen, (open) => {
  if (open) {
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
  } else document.body.style.overflow = previousOverflow;
});
watch(
  () => router.currentRoute?.value.fullPath,
  () => {
    menuOpen.value = false;
  },
);
onBeforeUnmount(() => {
  mobileNavigationMedia?.removeEventListener("change", closeDesktopNavigation);
  if (menuOpen.value) document.body.style.overflow = previousOverflow;
});

async function toggleNavigation() {
  menuOpen.value = !menuOpen.value;
  if (menuOpen.value) {
    await nextTick();
    navigation.value?.querySelector<HTMLElement>("a, button")?.focus();
  }
}
function handleNavigationKeydown(event: KeyboardEvent) {
  if (!menuOpen.value) return;
  if (event.key === "Escape") {
    event.preventDefault();
    menuOpen.value = false;
    menuToggle.value?.focus();
  }
  if (event.key === "Tab") {
    const controls = [
      menuToggle.value,
      ...Array.from(
        navigation.value?.querySelectorAll<HTMLElement>(
          "a, button:not(:disabled)",
        ) ?? [],
      ),
    ];
    const first = controls[0];
    const last = controls[controls.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  }
}

async function handleLogout() {
  if (loggingOut.value) return;
  logoutError.value = "";
  loggingOut.value = true;
  try {
    await logout();
    await router.push("/login");
  } catch {
    logoutError.value =
      "로그아웃 요청을 완료하지 못했습니다. 연결을 확인하고 다시 시도해 주세요.";
  } finally {
    loggingOut.value = false;
  }
}
</script>

<template>
  <div
    class="app-layout"
    :class="{
      'learner-workspace': learnerWorkspace,
      'navigation-open': menuOpen,
    }"
    @keydown="handleNavigationKeydown"
  >
    <a class="skip-link" href="#main-content">본문으로 건너뛰기</a>
    <header class="app-header">
      <RouterLink
        class="brand"
        :to="currentMember?.role === 'ADMIN' ? '/admin' : currentMember ? '/' : '/about'"
      >
        <span class="brand-mark" aria-hidden="true">c:</span
        ><span>crackcs<span class="brand-dot">.</span></span>
      </RouterLink>
      <button
        ref="menuToggle"
        class="navigation-toggle"
        type="button"
        aria-controls="primary-navigation"
        :aria-expanded="menuOpen"
        :aria-label="menuOpen ? '메뉴 닫기' : '메뉴 열기'"
        @click="toggleNavigation"
      >
        <span /><span />
      </button>
      <div class="header-tools">
        <ThemeSwitch />
        <nav
          id="primary-navigation"
          ref="navigation"
          class="header-actions"
          aria-label="주요 메뉴"
          @click="menuOpen = false"
        >
          <template v-if="currentMember">
            <span class="member-caption">{{ currentMember.nickname }}</span>
            <template v-if="currentMember.role === 'USER'">
              <RouterLink to="/">학습 홈</RouterLink>
              <RouterLink to="/questions">문제집</RouterLink>
              <RouterLink to="/knowledge-map">지식 지도</RouterLink>
            </template>
            <RouterLink v-if="currentMember.role === 'USER'" to="/answers"
              >답변 이력</RouterLink
            >
            <RouterLink v-if="currentMember.role === 'ADMIN'" to="/admin"
              >관리</RouterLink
            >
            <button
              class="header-text-button"
              type="button"
              :disabled="loggingOut"
              @click="handleLogout"
            >
              {{ loggingOut ? "로그아웃 중" : "로그아웃" }}
            </button>
          </template>
          <template v-else>
            <RouterLink to="/about">서비스 소개</RouterLink>
            <RouterLink class="header-login" to="/login">로그인</RouterLink>
            <RouterLink class="header-join" to="/sign-up">시작하기</RouterLink>
          </template>
        </nav>
        <p v-if="logoutError" class="form-alert" role="alert">
          {{ logoutError }}
        </p>
      </div>
    </header>
    <div class="workspace-body">
      <div v-if="learnerWorkspace" class="workspace-topbar">
        <span
          >나의 학습 공간 <span aria-hidden="true">/</span>
          <strong>{{ pageTitle }}</strong></span
        ><span class="workspace-member"
          ><span class="avatar" aria-hidden="true">{{
            currentMember?.nickname.slice(0, 1)
          }}</span
          >{{ currentMember?.nickname }}</span
        >
      </div>
      <div id="main-content" tabindex="-1"><RouterView /></div>
      <footer class="app-footer">
        <RouterLink to="/about">crackCS · 서비스 소개</RouterLink><span>한 질문씩, 내 언어로.</span>
      </footer>
    </div>
  </div>
</template>
