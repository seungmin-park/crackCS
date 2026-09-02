<script setup lang="ts">
import QuestionState from "@/components/QuestionState.vue";
</script>

<template>
  <main class="page-shell">
    <QuestionState kind="error" title="접근할 수 없는 화면입니다" description="관리자 권한이 있는 계정으로 로그인해 주세요." />
    <RouterLink class="back-link" to="/questions">학습 문제로 돌아가기</RouterLink>
  </main>
</template>
