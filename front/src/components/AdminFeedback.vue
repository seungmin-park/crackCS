<script setup lang="ts">
defineProps<{ success?: string; error?: string; fieldErrors?: Record<string, string> }>();
</script>

<template>
  <p v-if="success" class="admin-feedback success" role="status">{{ success }}</p>
  <div v-else-if="error" class="admin-feedback error" role="alert">
    <p>{{ error }}</p>
    <ul v-if="fieldErrors && Object.keys(fieldErrors).length"><li v-for="(reason, field) in fieldErrors" :key="field">{{ reason }}</li></ul>
  </div>
</template>
