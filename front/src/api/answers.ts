import { get, post } from "@/api/client";
import type { PageResponse } from "@/api/pagination";
import type { QuestionDifficulty } from "@/api/questions";

export type EvaluationStatus = "EVALUATING" | "PROCESSING" | "EVALUATED" | "NEEDS_REVIEW" | "FAILED";
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
  strengths: string[];
  omissions: string[];
  misconceptions: string[];
  evidence: EvaluationEvidence[];
};

export type EvaluationEvidence = {
  chunkId: number;
  documentTitle: string;
  documentVersion: number;
  startOffset: number;
  endOffset: number;
  content: string;
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

export type FollowUpQuestionStatus = "PENDING" | "PROCESSING" | "READY" | "FAILED" | "UNAVAILABLE";

export type FollowUpQuestionReason =
  | "EVALUATION_NOT_ELIGIBLE"
  | "FOLLOW_UP_LIMIT"
  | "CONTENT_UNAVAILABLE"
  | "PROVIDER_TIMEOUT"
  | "INVALID_RESULT"
  | "PROVIDER_ERROR"
  | "ATTEMPTS_EXHAUSTED"
  | "PERSISTENCE_ERROR";

export type FollowUpQuestion = {
  id: number;
  topic: { id: number; code: string; name: string };
  difficulty: QuestionDifficulty;
  content: string;
};

export type FollowUpQuestionResponse =
  | { status: "PENDING" | "PROCESSING"; reason: FollowUpQuestionReason | null; question: null }
  | { status: "READY"; reason: null; question: FollowUpQuestion }
  | { status: "FAILED" | "UNAVAILABLE"; reason: FollowUpQuestionReason; question: null };

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

export function fetchFollowUpQuestion(answerId: number | string): Promise<FollowUpQuestionResponse> {
  return get<FollowUpQuestionResponse>(`/api/answers/${encodeURIComponent(answerId)}/follow-up-question`);
}
