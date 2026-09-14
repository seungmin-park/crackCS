import { get } from "@/api/client";
import { queryString, type PageResponse } from "@/api/pagination";

export type AdminEvaluationStatus = "FAILED" | "NEEDS_REVIEW";

export type AdminEvaluationSummary = {
  evaluationId: number;
  answerId: number;
  questionId: number;
  status: AdminEvaluationStatus;
  failureCode: string | null;
  modelName: string | null;
  evaluatorVersion: string | null;
  occurredAt: string | null;
};

export type AdminEvaluationEvidence = {
  chunkId: number;
  documentTitle: string;
  documentVersion: number;
  startOffset: number;
  endOffset: number;
  content: string;
};

export type AdminEvaluationDetail = AdminEvaluationSummary & {
  questionContent: string;
  answerContent: string;
  evidence: AdminEvaluationEvidence[];
};

export function fetchAdminEvaluations(filters: { status?: AdminEvaluationStatus | ""; page?: number; size?: number } = {}): Promise<PageResponse<AdminEvaluationSummary>> {
  return get(`/api/admin/evaluations${queryString(filters)}`);
}

export function fetchAdminEvaluation(id: number): Promise<AdminEvaluationDetail> {
  return get(`/api/admin/evaluations/${id}`);
}
