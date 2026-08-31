<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";

import { signUp } from "@/api/auth";
import { ApiClientError } from "@/api/client";

const router = useRouter();
const form = reactive({ email: "", password: "", nickname: "" });
const fieldErrors = reactive<Record<string, string>>({});
const generalError = ref("");
const submitting = ref(false);

async function submit() {
  Object.keys(fieldErrors).forEach((field) => delete fieldErrors[field]);
  generalError.value = "";
  submitting.value = true;

  try {
    await signUp({ ...form });
    await router.push({ name: "login", query: { registered: "true" } });
  } catch (error) {
    if (error instanceof ApiClientError) {
      error.fieldErrors.forEach(({ field, reason }) => {
        fieldErrors[field] = reason;
      });
      if (error.fieldErrors.length === 0) {
        generalError.value = error.message;
      }
    } else {
      generalError.value = "회원가입 요청을 처리하지 못했습니다.";
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="auth-shell">
    <section class="auth-story" aria-labelledby="sign-up-title">
      <p class="eyebrow">START YOUR LOOP</p>
      <h1 id="sign-up-title">아는 것과<br />설명할 수 있는 것 사이.</h1>
      <p>
        서술형 질문에 답하며 이해의 빈틈을 찾고, 다음 학습으로 이어지는 개인 지식 지도를 만드세요.
      </p>
      <ol class="auth-steps" aria-label="학습 흐름">
        <li><span>01</span> 질문을 읽고 자신의 언어로 답하기</li>
        <li><span>02</span> 핵심 개념 단위로 이해도 확인하기</li>
        <li><span>03</span> 취약 개념에 맞는 다음 질문 받기</li>
      </ol>
    </section>

    <section class="auth-card" aria-label="회원가입 양식">
      <div class="auth-card-heading">
        <span class="auth-index">ACCOUNT / 01</span>
        <h2>학습 계정 만들기</h2>
        <p>긴 passphrase를 사용하면 외우기 쉽고 추측하기 어렵습니다.</p>
      </div>

      <form novalidate @submit.prevent="submit">
        <label class="form-field">
          <span>이메일</span>
          <input
            v-model="form.email"
            type="email"
            name="email"
            autocomplete="email"
            placeholder="you@example.com"
            :aria-invalid="Boolean(fieldErrors.email)"
            :aria-describedby="fieldErrors.email ? 'email-error' : undefined"
          />
          <small v-if="fieldErrors.email" id="email-error" class="field-error">{{ fieldErrors.email }}</small>
        </label>

        <label class="form-field">
          <span>비밀번호</span>
          <input
            v-model="form.password"
            type="password"
            name="password"
            autocomplete="new-password"
            placeholder="15자 이상의 passphrase"
            :aria-invalid="Boolean(fieldErrors.password)"
            :aria-describedby="fieldErrors.password ? 'password-error' : 'password-help'"
          />
          <small v-if="fieldErrors.password" id="password-error" class="field-error">{{ fieldErrors.password }}</small>
          <small v-else id="password-help" class="field-help">15~64자, UTF-8 기준 72 byte 이하</small>
        </label>

        <label class="form-field">
          <span>닉네임</span>
          <input
            v-model="form.nickname"
            type="text"
            name="nickname"
            autocomplete="nickname"
            placeholder="학습 기록에 표시할 이름"
            :aria-invalid="Boolean(fieldErrors.nickname)"
            :aria-describedby="fieldErrors.nickname ? 'nickname-error' : undefined"
          />
          <small v-if="fieldErrors.nickname" id="nickname-error" class="field-error">{{ fieldErrors.nickname }}</small>
        </label>

        <p v-if="generalError" class="form-alert" role="alert">{{ generalError }}</p>

        <button class="auth-submit" type="submit" :disabled="submitting">
          {{ submitting ? "계정을 만들고 있어요" : "학습 시작하기" }}
        </button>
      </form>

      <p class="auth-switch">이미 계정이 있나요? <RouterLink to="/login">로그인</RouterLink></p>
    </section>
  </main>
</template>
