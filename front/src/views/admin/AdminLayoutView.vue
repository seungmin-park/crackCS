<script setup lang="ts">
import { RouterLink, RouterView } from "vue-router";

import QuestionState from "@/components/QuestionState.vue";
import { useAuth } from "@/composables/useAuth";

const { currentMember, authenticationResolved } = useAuth();
</script>

<template>
  <main class="admin-workspace">
    <QuestionState
      v-if="!authenticationResolved"
      kind="loading"
      title="관리자 권한을 확인하고 있어요"
      description="인증 상태를 복구한 뒤 관리 도구를 엽니다."
    />
    <QuestionState
      v-else-if="currentMember?.role !== 'ADMIN'"
      kind="error"
      title="관리자 권한이 필요합니다"
      description="콘텐츠 운영 화면은 ADMIN 회원만 사용할 수 있습니다."
    />
    <template v-else>
      <aside class="admin-sidebar">
        <RouterLink class="admin-home-link" to="/admin">crackCS / 운영</RouterLink>
        <nav aria-label="관리자 메뉴">
          <RouterLink to="/admin/taxonomy">분류와 개념</RouterLink>
          <RouterLink to="/admin/knowledge-documents">근거 문서</RouterLink>
          <RouterLink to="/admin/questions">문제</RouterLink>
          <RouterLink to="/admin/members">회원</RouterLink>
        </nav>
        <small>분류부터 검수·공개까지, 콘텐츠를 관리합니다.</small>
      </aside>
      <section class="admin-content"><RouterView /></section>
    </template>
  </main>
</template>
