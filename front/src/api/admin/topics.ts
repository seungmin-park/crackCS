import { get, patch, post } from "@/api/client";
import { queryString, type PageRequest, type PageResponse } from "@/api/pagination";

export type Topic = {
  id: number;
  parentId: number | null;
  code: string;
  name: string;
  active: boolean;
};

export type TopicInput = { parentId?: number; code: string; name: string };

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
