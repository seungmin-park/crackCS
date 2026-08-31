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
      <p class="eyebrow">WELCOME BACK</p>
      <h1 id="login-title">오늘의 질문이<br />생각을 단단하게.</h1>
      <p>지난 이해 상태를 이어받아 지금 가장 먼저 설명해 볼 문제부터 시작합니다.</p>
      <blockquote class="auth-quote">
        <span aria-hidden="true">“</span>
        외운 문장은 사라지지만, 자신의 말로 설명한 구조는 오래 남습니다.
      </blockquote>
    </section>

    <section class="auth-card" aria-label="로그인 양식">
      <div class="auth-card-heading">
        <span class="auth-index">SESSION / 01</span>
        <h2>다시 이어서 학습하기</h2>
        <p>브라우저에는 인증 token 대신 안전한 HttpOnly session만 남습니다.</p>
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
            placeholder="가입할 때 만든 passphrase"
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
