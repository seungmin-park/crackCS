import { get, patch, post } from "@/api/client";
import { queryString, type PageRequest, type PageResponse } from "@/api/pagination";
import type { ContentStatus } from "./types";

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

export type KnowledgeChunk = {
  id: number;
  sequenceNo: number;
  startOffset: number;
  endOffset: number;
  content: string;
  checksum: string;
  generationKey: string;
  searchStatus: "KEYWORD_SEARCHABLE" | "EMBEDDING_PENDING" | "EMBEDDING_FAILED" | "READY";
};

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

export function generateKnowledgeChunks(id: number): Promise<{ generationKey: string; reused: boolean; chunks: KnowledgeChunk[] }> {
  return post(`/api/admin/knowledge-documents/${id}/chunks`);
}

export function fetchKnowledgeChunks(id: number): Promise<KnowledgeChunk[]> {
  return get(`/api/admin/knowledge-documents/${id}/chunks`);
}
