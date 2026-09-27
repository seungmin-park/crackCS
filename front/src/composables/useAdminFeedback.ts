import { computed, ref } from "vue";

import { presentRequestError } from "@/presentation/requestErrorPresentation";

export function useAdminFeedback() {
  const successMessage = ref("");
  const formError = ref("");
  const fieldErrors = ref<Record<string, string>>({});
  const submitting = ref(false);

  const hasFeedback = computed(() => Boolean(successMessage.value || formError.value));

  async function execute<T>(action: () => Promise<T>, success: string): Promise<T | undefined> {
    if (submitting.value) return undefined;
    successMessage.value = "";
    formError.value = "";
    fieldErrors.value = {};
    submitting.value = true;
    try {
      const result = await action();
      successMessage.value = success;
      return result;
    } catch (error) {
      const failure = presentRequestError(error);
      formError.value = failure.message;
      fieldErrors.value = failure.fieldErrors;
      return undefined;
    } finally {
      submitting.value = false;
    }
  }

  return { successMessage, formError, fieldErrors, submitting, hasFeedback, execute };
}
