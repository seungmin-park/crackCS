<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  actionLabel?: string;
}>();

defineEmits<{
  action: [];
}>();
</script>

<template>
  <section class="state-panel" role="status">
    <span class="state-symbol" aria-hidden="true">?</span>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <button v-if="actionLabel" class="primary-button" type="button" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </section>
</template>
