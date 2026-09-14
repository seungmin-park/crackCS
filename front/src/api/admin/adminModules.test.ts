import { beforeEach, describe, expect, it, vi } from "vitest";

import { fetchConcepts } from "./concepts";
import { fetchAdminEvaluations } from "./evaluations";
import { fetchKnowledgeDocuments } from "./knowledgeDocuments";
import { fetchAdminMembers } from "./members";
import { fetchAdminQuestions } from "./questions";
import { fetchTopics } from "./topics";

const { get } = vi.hoisted(() => ({ get: vi.fn() }));
vi.mock("@/api/client", () => ({ get: get }));

describe("관리자 목록 API 계약", () => {
  beforeEach(() => get.mockReset());

  it.each([
    ["Topic", () => fetchTopics({ active: false, page: 2, size: 10 }), "/api/admin/topics", { active: "false", page: "2", size: "10" }],
    ["Concept", () => fetchConcepts({ topicId: 3, active: true, size: 30 }), "/api/admin/concepts", { topicId: "3", active: "true", size: "30" }],
    ["근거 문서", () => fetchKnowledgeDocuments({ topicId: 4, status: "DRAFT", technologyVersion: "21", sort: "id,desc" }), "/api/admin/knowledge-documents", { topicId: "4", status: "DRAFT", technologyVersion: "21", sort: "id,desc" }],
    ["Question", () => fetchAdminQuestions({ topicId: 5, status: "PUBLISHED", page: 1 }), "/api/admin/questions", { topicId: "5", status: "PUBLISHED", page: "1" }],
    ["회원", () => fetchAdminMembers({ status: "BLOCKED", size: 50 }), "/api/admin/members", { status: "BLOCKED", size: "50" }],
    ["평가", () => fetchAdminEvaluations({ status: "NEEDS_REVIEW", page: 3, size: 20 }), "/api/admin/evaluations", { status: "NEEDS_REVIEW", page: "3", size: "20" }],
  ] as const)("%s 필터를 해당 목록 endpoint의 query로 전달한다", async (_, request, pathname, expectedQuery) => {
    await request();

    const url = new URL(get.mock.calls[0]![0], "https://example.test");
    expect(url.pathname).toBe(pathname);
    expect(Object.fromEntries(url.searchParams)).toEqual(expectedQuery);
  });

  it("빈 평가 상태 필터는 query에 전송하지 않는다", async () => {
    await fetchAdminEvaluations({ status: "", page: 0, size: 20 });

    const url = new URL(get.mock.calls[0]![0], "https://example.test");
    expect(url.searchParams.has("status")).toBe(false);
    expect(url.searchParams.get("page")).toBe("0");
  });
});
