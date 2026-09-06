<script setup lang="ts">
import { onBeforeUnmount, ref } from "vue";

type Theme = "system" | "light" | "dark";
const preference = ref<Theme>("system");
const media = typeof matchMedia === "function" ? matchMedia("(prefers-color-scheme: dark)") : undefined;
try {
  const saved = localStorage.getItem("crackcs-theme");
  if (saved === "light" || saved === "dark") preference.value = saved;
} catch { /* Storage can be unavailable; the selector must still work. */ }

function applyTheme() {
  document.documentElement.dataset.theme = preference.value === "system"
    ? (media?.matches ? "dark" : "light") : preference.value;
}

function changeTheme() {
  applyTheme();
  try { localStorage.setItem("crackcs-theme", preference.value); } catch { /* Keep the in-page preference. */ }
}

applyTheme();
media?.addEventListener("change", applyTheme);
onBeforeUnmount(() => media?.removeEventListener("change", applyTheme));
</script>

<template>
  <label class="theme-switch">
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
      <circle cx="12" cy="12" r="8" /><path d="M12 4v16a8 8 0 0 0 0-16" fill="currentColor" stroke="none" />
    </svg>
    <select v-model="preference" aria-label="화면 테마" @change="changeTheme">
      <option value="system">시스템</option>
      <option value="light">화이트</option>
      <option value="dark">다크</option>
    </select>
  </label>
</template>
