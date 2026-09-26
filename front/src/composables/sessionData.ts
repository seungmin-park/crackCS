import { clearCsrfToken } from "@/api/client";
import { clearPendingAnswerSubmissions } from "@/composables/useAnswerSubmission";

export function clearSessionData(): void {
  clearPendingAnswerSubmissions();
  clearCsrfToken();
}
