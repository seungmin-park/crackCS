import { get, patch, post, put } from "@/api/client";
import { queryString, type PageRequest, type PageResponse } from "@/api/pagination";
import type { ContentStatus } from "./types";

export type QuestionDifficulty = "BASIC" | "INTERMEDIATE" | "ADVANCED";

export type QuestionConcept = {
  conceptId: number;
  code: string;
  name: string;
  weight: number;
  required: boolean;
};

export type AdminQuestion = {
  id: number;
  topicId: number;
  origin: "ADMIN" | "SYSTEM_FOLLOW_UP";
  type: "NORMAL" | "FOLLOW_UP";
  difficulty: QuestionDifficulty;
  content: string;
  referenceAnswer: string;
  status: ContentStatus;
  versionSeriesId: string;
  questionVersion: number;
  createdByMemberId: number | null;
  reviewedByMemberId: number | null;
  reviewedAt: string | null;
  concepts: QuestionConcept[];
  createdAt: string;
  updatedAt: string;
};

export type AdminQuestionSummary = Pick<
  AdminQuestion,
  "id" | "topicId" | "origin" | "difficulty" | "content" | "status" | "questionVersion" | "createdAt" | "updatedAt"
>;

export type AdminQuestionInput = {
  topicId: number;
  difficulty: QuestionDifficulty;
  content: string;
  referenceAnswer: string;
};

export function fetchAdminQuestions(filters: { topicId?: number; status?: ContentStatus } & PageRequest = {}): Promise<PageResponse<AdminQuestionSummary>> {
  return get(`/api/admin/questions${queryString(filters)}`);
}

export function fetchAdminQuestion(id: number): Promise<AdminQuestion> {
  return get(`/api/admin/questions/${id}`);
}

export function createAdminQuestion(input: AdminQuestionInput): Promise<AdminQuestion> {
  return post("/api/admin/questions", input);
}

export function updateAdminQuestion(id: number, input: AdminQuestionInput): Promise<AdminQuestion> {
  return patch(`/api/admin/questions/${id}`, input);
}

export function replaceQuestionConcepts(id: number, concepts: Array<{ conceptId: number; weight: number; required: boolean }>): Promise<AdminQuestion> {
  return put(`/api/admin/questions/${id}/concepts`, { concepts });
}

export function reviewQuestion(id: number): Promise<AdminQuestion> {
  return post(`/api/admin/questions/${id}/review`);
}

export function publishQuestion(id: number): Promise<AdminQuestion> {
  return post(`/api/admin/questions/${id}/publish`);
}

export function retireQuestion(id: number): Promise<AdminQuestion> {
  return post(`/api/admin/questions/${id}/retire`);
}

export function createQuestionVersion(id: number, input: Omit<AdminQuestionInput, "topicId">): Promise<AdminQuestion> {
  return post(`/api/admin/questions/${id}/versions`, input);
}
