<script setup lang="ts">
type QuestionStateKind = "loading" | "error" | "empty";

const props = withDefaults(defineProps<{
  kind?: QuestionStateKind;
  title: string;
  description: string;
  actionLabel?: string;
}>(), { kind: "empty" });

defineEmits<{
  action: [];
}>();
</script>

<template>
  <section
    class="state-panel"
    :data-kind="props.kind"
    :role="props.kind === 'error' ? 'alert' : 'status'"
    :aria-busy="props.kind === 'loading' ? true : undefined"
  >
    <span class="state-symbol" aria-hidden="true">?</span>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <button v-if="actionLabel" class="primary-button" type="button" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </section>
</template>
