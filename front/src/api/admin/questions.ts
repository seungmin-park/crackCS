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

export function fetchAdminQuestion(questionId: number): Promise<AdminQuestion> {
  return get(`/api/admin/questions/${questionId}`);
}

export function createAdminQuestion(input: AdminQuestionInput): Promise<AdminQuestion> {
  return post("/api/admin/questions", input);
}

export function updateAdminQuestion(questionId: number, input: AdminQuestionInput): Promise<AdminQuestion> {
  return patch(`/api/admin/questions/${questionId}`, input);
}

export function replaceQuestionConcepts(questionId: number, concepts: Array<{ conceptId: number; weight: number; required: boolean }>): Promise<AdminQuestion> {
  return put(`/api/admin/questions/${questionId}/concepts`, { concepts });
}

export function reviewQuestion(questionId: number): Promise<AdminQuestion> {
  return post(`/api/admin/questions/${questionId}/review`);
}

export function publishQuestion(questionId: number): Promise<AdminQuestion> {
  return post(`/api/admin/questions/${questionId}/publish`);
}

export function retireQuestion(questionId: number): Promise<AdminQuestion> {
  return post(`/api/admin/questions/${questionId}/retire`);
}

export function createQuestionVersion(questionId: number, input: Omit<AdminQuestionInput, "topicId">): Promise<AdminQuestion> {
  return post(`/api/admin/questions/${questionId}/versions`, input);
}
