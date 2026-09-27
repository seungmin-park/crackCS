<script setup lang="ts">
import QuestionState from "@/components/QuestionState.vue";
import type { RequestErrorPresentation } from "@/presentation/requestErrorPresentation";

withDefaults(defineProps<{ failure: RequestErrorPresentation; title: string; actionLabel?: string; retryKey?: string }>(), { actionLabel: "다시 불러오기" });
defineEmits<{ retry: [] }>();
</script>

<template>
  <QuestionState kind="error" :title="title" :description="failure.message"
    :retry-key="retryKey" :action-label="failure.retryable ? actionLabel : undefined" @action="$emit('retry')" />
</template>
