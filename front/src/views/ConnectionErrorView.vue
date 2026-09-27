<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import QuestionState from "@/components/QuestionState.vue";

const route = useRoute();
const router = useRouter();
const retrying = ref(false);
async function retryConnection() {
  if (retrying.value) return;
  retrying.value = true;
  const target = route.query.redirect;
  const path = typeof target === "string" && target.startsWith("/") && !target.startsWith("//")
    && !target.startsWith("/connection-error") ? target : "/";
  try { await router.replace(path); }
  catch { /* Keep the recovery screen if navigation itself fails. */ }
  finally { retrying.value = false; }
}
</script>

<template>
  <main class="page-shell">
    <QuestionState v-if="retrying" kind="loading" title="연결을 확인하고 있어요" description="잠시만 기다려 주세요." />
    <QuestionState v-else kind="error" title="서비스에 연결하지 못했어요" description="연결을 확인한 뒤 다시 시도해 주세요. 입력한 주소로 다시 이동합니다."
      action-label="다시 연결하기" @action="retryConnection" />
  </main>
</template>
