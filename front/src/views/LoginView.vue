<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { ApiClientError } from "@/api/client";
import { useAuth } from "@/composables/useAuth";

const route = useRoute();
const router = useRouter();
const { login } = useAuth();
const form = reactive({ email: "", password: "" });
const errorMessage = ref("");
const submitting = ref(false);
const registered = route.query.registered === "true";

async function submit() {
  errorMessage.value = "";
  submitting.value = true;

  try {
    await login({ ...form });
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/questions";
    await router.push(redirect);
  } catch (error) {
    errorMessage.value = error instanceof ApiClientError
      ? error.message
      : "로그인 요청을 처리하지 못했습니다.";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="auth-shell auth-shell-login">
    <section class="auth-story" aria-labelledby="login-title">
      <p class="eyebrow">다시 시작하는 공부</p>
      <h1 id="login-title">읽었던 개념을,<br />내 언어로.</h1>
      <p>CS와 백엔드 질문을 살펴보며, 어디까지 설명할 수 있는지 확인해 보세요.</p>
      <blockquote class="auth-quote">
        <span aria-hidden="true">“</span>
        프로세스와 스레드는 어떻게 다를까요?
        익숙한 질문 하나부터 생각을 정리해 보세요.
      </blockquote>
    </section>

    <section class="auth-card" aria-label="로그인 양식">
      <div class="auth-card-heading">
        <span class="auth-index">로그인</span>
        <h2>문제집 펼치기</h2>
        <p>가입한 이메일과 비밀번호로 로그인하세요.</p>
      </div>

      <p v-if="registered" class="form-success" role="status">
        계정이 만들어졌습니다. 이제 로그인해 주세요.
      </p>

      <form novalidate @submit.prevent="submit">
        <label class="form-field">
          <span>이메일</span>
          <input
            v-model="form.email"
            name="email"
            type="email"
            autocomplete="email"
            placeholder="you@example.com"
          />
        </label>

        <label class="form-field">
          <span>비밀번호</span>
          <input
            v-model="form.password"
            name="password"
            type="password"
            autocomplete="current-password"
            placeholder="비밀번호를 입력하세요"
          />
        </label>

        <p v-if="errorMessage" class="form-alert" role="alert">{{ errorMessage }}</p>

        <button class="auth-submit" type="submit" :disabled="submitting">
          {{ submitting ? "확인하고 있어요" : "로그인" }}
        </button>
      </form>

      <p class="auth-switch">처음 방문했나요? <RouterLink to="/sign-up">계정 만들기</RouterLink></p>
    </section>
  </main>
</template>
