import type { LocationQuery, LocationQueryRaw, Router } from "vue-router";

import type { PageResponse } from "@/api/pagination";

export const ADMIN_PAGE_SIZE = 20;
export const RELATION_PAGE_SIZE = 100;
const MAX_RELATION_PAGES = 1000;

export function queryPage(query: LocationQuery, key = "page"): number {
  const value = Number(query[key] ?? 0);
  return Number.isInteger(value) && value >= 0 ? value : 0;
}

export function queryStringValue(query: LocationQuery, key: string): string {
  const value = query[key];
  return typeof value === "string" ? value : "";
}

export async function updateAdminQuery(
  router: Router,
  query: LocationQuery,
  values: Record<string, string | number | undefined>,
): Promise<void> {
  const next: LocationQueryRaw = { ...query };
  Object.entries(values).forEach(([key, value]) => {
    if (value === undefined || value === "") delete next[key];
    else next[key] = String(value);
  });
  await router.push({ query: next });
}

export function normalizedPage(requestedPage: number, totalPages: number): number {
  return Math.min(requestedPage, Math.max(totalPages - 1, 0));
}

export async function replaceAdminQuery(
  router: Router,
  query: LocationQuery,
  values: Record<string, string | number | undefined>,
): Promise<void> {
  const next: LocationQueryRaw = { ...query };
  Object.entries(values).forEach(([key, value]) => {
    if (value === undefined || value === "") delete next[key];
    else next[key] = String(value);
  });
  await router.replace({ query: next });
}

export async function fetchAllPages<T>(
  fetchPage: (page: number, size: number) => Promise<PageResponse<T>>,
): Promise<T[]> {
  const first = await fetchPage(0, RELATION_PAGE_SIZE);
  if (first.totalPages > MAX_RELATION_PAGES) {
    throw new Error("관계 후보 페이지 수가 안전 상한을 초과했습니다.");
  }
  const totalPages = Math.max(first.totalPages, 1);
  const content = [...first.content];
  for (let page = 1; page < totalPages; page += 1) {
    const response = await fetchPage(page, RELATION_PAGE_SIZE);
    content.push(...response.content);
  }
  return content;
}
