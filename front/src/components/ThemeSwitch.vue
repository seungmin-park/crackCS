<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId } from "vue";

type Theme = "system" | "light" | "dark";
const themeOptions: { value: Theme; label: string; iconPath: string }[] = [
  { value: "system", label: "시스템", iconPath: "M4 4h16v12H4zM9 20h6M12 16v4" },
  { value: "light", label: "화이트", iconPath: "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8ZM12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5 19 19M5 19l1.5-1.5M17.5 6.5 19 5" },
  { value: "dark", label: "다크", iconPath: "M20.5 13A8.5 8.5 0 0 1 11 3.5 8.5 8.5 0 1 0 20.5 13Z" },
];
const preference = ref<Theme>("system");
const selectedOptionIndex = computed(() => themeOptions.findIndex(option => option.value === preference.value));
const selectedOption = computed(() => themeOptions[selectedOptionIndex.value]!);
const menuOpen = ref(false);
const root = ref<HTMLDivElement>();
const trigger = ref<HTMLButtonElement>();
const menu = ref<HTMLDivElement>();
const menuId = useId();
const media = typeof matchMedia === "function" ? matchMedia("(prefers-color-scheme: dark)") : undefined;
try {
  const saved = localStorage.getItem("crackcs-theme");
  if (saved === "light" || saved === "dark") preference.value = saved;
} catch { /* Storage can be unavailable; the selector must still work. */ }

function applyTheme() {
  document.documentElement.dataset.theme = preference.value === "system"
    ? (media?.matches ? "dark" : "light") : preference.value;
}

function closeMenu() {
  menuOpen.value = false;
}

function chooseTheme(theme: Theme) {
  preference.value = theme;
  applyTheme();
  try { localStorage.setItem("crackcs-theme", theme); } catch { /* Keep the in-page preference. */ }
  closeMenu();
  trigger.value?.focus({ preventScroll: true });
}

function focusOption(index: number) {
  menu.value?.querySelectorAll<HTMLButtonElement>('[role="menuitemradio"]')[index]?.focus();
}

async function openMenu(index = selectedOptionIndex.value) {
  menuOpen.value = true;
  await nextTick();
  focusOption(index);
}

function toggleMenu() {
  if (menuOpen.value) closeMenu();
  else void openMenu();
}

function handleTriggerKeydown(event: KeyboardEvent) {
  if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;
  event.preventDefault();
  void openMenu(event.key === "ArrowDown" ? 0 : themeOptions.length - 1);
}

function handleOptionKeydown(event: KeyboardEvent, index: number) {
  if (event.key === "Tab") {
    closeMenu();
    return;
  }
  switch (event.key) {
    case "Escape":
      closeMenu();
      trigger.value?.focus({ preventScroll: true });
      break;
    case "ArrowDown":
      focusOption((index + 1) % themeOptions.length);
      break;
    case "ArrowUp":
      focusOption((index + themeOptions.length - 1) % themeOptions.length);
      break;
    case "Home":
      focusOption(0);
      break;
    case "End":
      focusOption(themeOptions.length - 1);
      break;
    case "Enter":
    case " ":
      chooseTheme(themeOptions[index]!.value);
      break;
    default:
      return;
  }
  event.preventDefault();
}

function closeOutside(event: PointerEvent) {
  if (menuOpen.value && event.target instanceof Node && !root.value?.contains(event.target)) closeMenu();
}

applyTheme();
media?.addEventListener("change", applyTheme);
onMounted(() => document.addEventListener("pointerdown", closeOutside, true));
onBeforeUnmount(() => {
  media?.removeEventListener("change", applyTheme);
  document.removeEventListener("pointerdown", closeOutside, true);
});
</script>

<template>
  <div ref="root" class="theme-switch">
    <button ref="trigger" class="theme-trigger" type="button"
      :aria-label="`화면 테마: ${selectedOption.label}`" aria-haspopup="menu"
      :aria-expanded="menuOpen" :aria-controls="menuId"
      @click="toggleMenu" @keydown="handleTriggerKeydown">
      <svg class="theme-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
        <path :d="selectedOption.iconPath" />
      </svg>
      <span>{{ selectedOption.label }}</span>
      <svg class="theme-chevron" aria-hidden="true" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="m4 6 4 4 4-4" /></svg>
    </button>
    <div v-if="menuOpen" :id="menuId" ref="menu" class="theme-menu" role="menu" aria-label="화면 테마 선택">
      <button v-for="(option, index) in themeOptions" :key="option.value"
        class="theme-option" type="button" role="menuitemradio" :aria-checked="preference === option.value"
        :data-theme-choice="option.value" tabindex="-1"
        @click="chooseTheme(option.value)" @keydown="handleOptionKeydown($event, index)">
        <svg class="theme-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path :d="option.iconPath" /></svg>
        <span>{{ option.label }}</span>
        <svg class="theme-check" aria-hidden="true" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m3 8 3 3 7-7" /></svg>
      </button>
    </div>
  </div>
</template>
