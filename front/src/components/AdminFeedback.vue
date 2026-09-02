<script setup lang="ts">
defineProps<{ success?: string; error?: string }>();
</script>

<template>
  <p v-if="success" class="admin-feedback success" role="status">{{ success }}</p>
  <p v-else-if="error" class="admin-feedback error" role="alert">{{ error }}</p>
</template>
