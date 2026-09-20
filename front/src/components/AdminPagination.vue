<script setup lang="ts">
defineProps<{ page: number; totalPages: number; totalElements: number }>();
defineEmits<{ change: [page: number] }>();
</script>

<template>
  <nav v-if="totalPages > 1" class="admin-pagination" aria-label="목록 페이지">
    <button type="button" data-page="previous" :disabled="page <= 0" @click="$emit('change', page - 1)">이전</button>
    <span>{{ page + 1 }} / {{ totalPages }} · 총 {{ totalElements }}개</span>
    <button type="button" data-page="next" :disabled="page + 1 >= totalPages" @click="$emit('change', page + 1)">다음</button>
  </nav>
</template>
