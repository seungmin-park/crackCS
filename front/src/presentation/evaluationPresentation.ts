import type { EvaluationStatus, EvaluationVerdict } from "@/api/answers";

export type EvaluationPresentationKind = "evaluating" | "failed" | "review" | "result";

export type EvaluationPresentation = {
  kind: EvaluationPresentationKind;
  label: string;
};

const verdictLabels: Record<EvaluationVerdict, string> = {
  CORRECT: "정답",
  PARTIALLY_CORRECT: "부분 정답",
  INCORRECT: "오답",
  NEEDS_REVIEW: "검토 필요",
};

export function presentEvaluation(
  status: EvaluationStatus,
  verdict: EvaluationVerdict | null,
): EvaluationPresentation {
  if (status === "EVALUATING" || status === "PROCESSING") {
    return { kind: "evaluating", label: "평가 중" };
  }
  if (status === "FAILED") {
    return { kind: "failed", label: "평가 실패" };
  }
  if (status === "NEEDS_REVIEW" || verdict === "NEEDS_REVIEW") {
    return { kind: "review", label: "검토 필요" };
  }
  return { kind: "result", label: verdict ? verdictLabels[verdict] : "평가 결과 없음" };
}
