import { get, patch, post, put } from "@/api/client";
import { queryString, type PageRequest, type PageResponse } from "@/api/pagination";

export type ContentStatus = "DRAFT" | "PUBLISHED" | "RETIRED";

export type Topic = {
  id: number;
  parentId: number | null;
  code: string;
  name: string;
  active: boolean;
};

export type TopicInput = { parentId?: number; code: string; name: string };

export type Concept = {
  id: number;
  topicId: number;
  code: string;
  name: string;
  description: string | null;
  active: boolean;
};

export type ConceptInput = {
  topicId: number;
  code: string;
  name: string;
  description?: string;
};

export type KnowledgeSourceType = "OFFICIAL_SPEC" | "OFFICIAL_DOC" | "INTERNAL_SUMMARY";

export type KnowledgeDocument = {
  id: number;
  topicId: number;
  createdByMemberId: number;
  reviewedByMemberId: number | null;
  title: string;
  sourceType: KnowledgeSourceType;
  sourceUrl: string | null;
  versionSeriesId: string;
  documentVersion: number;
  technologyVersion: string | null;
  licenseNote: string | null;
  content: string;
  checksum: string;
  status: ContentStatus;
  reviewedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type KnowledgeDocumentInput = {
  topicId: number;
  title: string;
  sourceType: KnowledgeSourceType;
  sourceUrl?: string;
  technologyVersion?: string;
  licenseNote?: string;
  content: string;
};

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

export type MemberStatus = "ACTIVE" | "BLOCKED" | "WITHDRAWN";
export type AdminMember = {
  id: number;
  nickname: string;
  role: "USER" | "ADMIN";
  status: MemberStatus;
};

export type KnowledgeChunk = {
  id: number; sequenceNo: number; startOffset: number; endOffset: number; content: string;
  checksum: string; generationKey: string; searchStatus: "KEYWORD_SEARCHABLE" | "EMBEDDING_PENDING" | "EMBEDDING_FAILED" | "READY";
};

export type AdminEvaluationStatus = "FAILED" | "NEEDS_REVIEW";
export type AdminEvaluationSummary = {
  evaluationId: number; answerId: number; questionId: number; status: AdminEvaluationStatus;
  failureCode: string | null; modelName: string | null; evaluatorVersion: string | null; occurredAt: string;
};
export type AdminEvaluationEvidence = {
  chunkId: number; documentTitle: string; documentVersion: number; startOffset: number; endOffset: number; content: string;
};
export type AdminEvaluationDetail = AdminEvaluationSummary & {
  questionContent: string; answerContent: string; evidence: AdminEvaluationEvidence[];
};

export function fetchTopics(filters: { active?: boolean } & PageRequest = {}): Promise<PageResponse<Topic>> {
  return get(`/api/admin/topics${queryString(filters)}`);
}

export function createTopic(input: TopicInput): Promise<Topic> {
  return post("/api/admin/topics", input);
}

export function updateTopic(topicId: number, input: TopicInput): Promise<Topic> {
  return patch(`/api/admin/topics/${topicId}`, input);
}

export function deactivateTopic(topicId: number): Promise<void> {
  return post(`/api/admin/topics/${topicId}/deactivate`);
}

export function fetchConcepts(filters: { topicId?: number; active?: boolean } & PageRequest = {}): Promise<PageResponse<Concept>> {
  return get(`/api/admin/concepts${queryString(filters)}`);
}

export function createConcept(input: ConceptInput): Promise<Concept> {
  return post("/api/admin/concepts", input);
}

export function updateConcept(conceptId: number, input: ConceptInput): Promise<Concept> {
  return patch(`/api/admin/concepts/${conceptId}`, input);
}

export function deactivateConcept(conceptId: number): Promise<void> {
  return post(`/api/admin/concepts/${conceptId}/deactivate`);
}

export function fetchKnowledgeDocuments(filters: {
  topicId?: number;
  status?: ContentStatus;
  technologyVersion?: string;
} & PageRequest = {}): Promise<PageResponse<KnowledgeDocument>> {
  return get(`/api/admin/knowledge-documents${queryString(filters)}`);
}

export function createKnowledgeDocument(input: KnowledgeDocumentInput): Promise<KnowledgeDocument> {
  return post("/api/admin/knowledge-documents", input);
}

export function updateKnowledgeDocument(id: number, input: KnowledgeDocumentInput): Promise<KnowledgeDocument> {
  return patch(`/api/admin/knowledge-documents/${id}`, input);
}

export function createKnowledgeDocumentVersion(id: number, input: KnowledgeDocumentInput): Promise<KnowledgeDocument> {
  return post(`/api/admin/knowledge-documents/${id}/versions`, input);
}

export function reviewKnowledgeDocument(id: number): Promise<KnowledgeDocument> {
  return post(`/api/admin/knowledge-documents/${id}/review`);
}

export function publishKnowledgeDocument(id: number): Promise<KnowledgeDocument> {
  return post(`/api/admin/knowledge-documents/${id}/publish`);
}

export function retireKnowledgeDocument(id: number): Promise<KnowledgeDocument> {
  return post(`/api/admin/knowledge-documents/${id}/retire`);
}

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

export function fetchAdminMembers(filters: { status?: MemberStatus } & PageRequest = {}): Promise<PageResponse<AdminMember>> {
  return get(`/api/admin/members${queryString(filters)}`);
}

export function updateMemberStatus(id: number, status: MemberStatus): Promise<AdminMember> {
  return patch(`/api/admin/members/${id}/status`, { status });
}

export function generateKnowledgeChunks(id: number): Promise<{ generationKey: string; reused: boolean; chunks: KnowledgeChunk[] }> {
  return post(`/api/admin/knowledge-documents/${id}/chunks`);
}

export function fetchKnowledgeChunks(id: number): Promise<KnowledgeChunk[]> {
  return get(`/api/admin/knowledge-documents/${id}/chunks`);
}

export function fetchAdminEvaluations(filters: { status?: AdminEvaluationStatus | ""; page?: number; size?: number } = {}): Promise<PageResponse<AdminEvaluationSummary>> {
  return get(`/api/admin/evaluations${queryString(filters)}`);
}

export function fetchAdminEvaluation(id: number): Promise<AdminEvaluationDetail> {
  return get(`/api/admin/evaluations/${id}`);
}
