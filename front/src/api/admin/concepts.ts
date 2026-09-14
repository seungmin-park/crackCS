import { get, patch, post } from "@/api/client";
import { queryString, type PageRequest, type PageResponse } from "@/api/pagination";

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
