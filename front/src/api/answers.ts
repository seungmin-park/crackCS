import { get, post } from "@/api/client";
import type { PageResponse } from "@/api/pagination";

export type EvaluationStatus = "EVALUATING" | "EVALUATED" | "FAILED";
export type EvaluationVerdict = "CORRECT" | "PARTIALLY_CORRECT" | "INCORRECT" | "NEEDS_REVIEW";

export type ConceptEvaluation = {
  conceptId: number;
  conceptName: string;
  verdict: EvaluationVerdict;
  score: number | null;
  feedback: string | null;
};

export type AnswerEvaluation = {
  status: EvaluationStatus;
  verdict: EvaluationVerdict | null;
  score: number | null;
  feedback: string | null;
  failureReason: string | null;
  concepts: ConceptEvaluation[];
};

export type AnswerResponse = {
  answerId: number;
  evaluationId: number;
  questionId: number;
  questionContent: string;
  content: string;
  submittedAt: string;
  evaluation: AnswerEvaluation;
};

export type SubmitAnswerRequest = { requestId: string; content: string };

export function submitAnswer(questionId: number | string, request: SubmitAnswerRequest): Promise<AnswerResponse> {
  return post<AnswerResponse>(`/api/questions/${encodeURIComponent(questionId)}/answers`, { content: request.content }, { "Idempotency-Key": request.requestId });
}

export function fetchMyAnswers(options: { page?: number; size?: number } = {}): Promise<PageResponse<AnswerResponse>> {
  const query = new URLSearchParams({ page: String(options.page ?? 0), size: String(options.size ?? 20) });
  return get<PageResponse<AnswerResponse>>(`/api/members/me/answers?${query}`);
}

export function fetchAnswer(answerId: number | string): Promise<AnswerResponse> {
  return get<AnswerResponse>(`/api/answers/${encodeURIComponent(answerId)}`);
}

export function fetchAnswerEvaluation(answerId: number | string): Promise<AnswerEvaluation> {
  return get<AnswerEvaluation>(`/api/answers/${encodeURIComponent(answerId)}/evaluation`);
}
