import { computed, ref } from "vue";

import { ApiClientError } from "@/api/client";

export function useAdminFeedback() {
  const successMessage = ref("");
  const formError = ref("");
  const fieldErrors = ref<Record<string, string>>({});
  const submitting = ref(false);

  const hasFeedback = computed(() => Boolean(successMessage.value || formError.value));

  async function execute<T>(action: () => Promise<T>, success: string): Promise<T | undefined> {
    successMessage.value = "";
    formError.value = "";
    fieldErrors.value = {};
    submitting.value = true;
    try {
      const result = await action();
      successMessage.value = success;
      return result;
    } catch (error) {
      if (error instanceof ApiClientError) {
        formError.value = error.message;
        fieldErrors.value = Object.fromEntries(error.fieldErrors.map((item) => [item.field, item.reason]));
      } else {
        formError.value = "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
      }
      return undefined;
    } finally {
      submitting.value = false;
    }
  }

  return { successMessage, formError, fieldErrors, submitting, hasFeedback, execute };
}
