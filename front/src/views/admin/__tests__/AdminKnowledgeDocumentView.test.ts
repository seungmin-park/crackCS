import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

const api = vi.hoisted(() => ({
  fetchTopics: vi.fn(), fetchKnowledgeDocuments: vi.fn(), fetchKnowledgeChunks: vi.fn(),
  updateKnowledgeDocument: vi.fn(), createKnowledgeDocument: vi.fn(), createKnowledgeDocumentVersion: vi.fn(),
  reviewKnowledgeDocument: vi.fn(), publishKnowledgeDocument: vi.fn(), retireKnowledgeDocument: vi.fn(), generateKnowledgeChunks: vi.fn(),
}));
vi.mock("@/api/admin", async importOriginal => ({ ...(await importOriginal<typeof import("@/api/admin")>()), ...api }));
import AdminKnowledgeDocumentView from "@/views/admin/AdminKnowledgeDocumentView.vue";

const document = { id: 1, topicId: 2, title: "기존 문서", sourceType: "OFFICIAL_DOC", sourceUrl: null, technologyVersion: null, licenseNote: null, content: "본문", status: "PUBLISHED", checksum: "sum", versionSeriesId: "series", documentVersion: 1 };

describe("관리자 근거 문서 화면", () => {
  beforeEach(() => {
    Object.values(api).forEach(mock => mock.mockReset());
    api.fetchTopics.mockResolvedValue({ content: [{ id: 2, name: "네트워크", active: true }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    api.fetchKnowledgeDocuments.mockResolvedValue({ content: [document], page: 0, size: 100, totalElements: 1, totalPages: 1 });
    api.fetchKnowledgeChunks.mockResolvedValue([]);
  });

  it("초기 목록 실패 뒤 재시도하면 로딩을 끝내고 목록을 표시한다", async () => {
    api.fetchKnowledgeDocuments.mockRejectedValueOnce(new Error("temporary"));
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    expect(wrapper.text()).toContain("문서 목록을 불러오지 못했습니다.");
    expect(wrapper.text()).not.toContain("문서를 불러오는 중");
    await wrapper.get("button[data-retry='list']").trigger("click");
    await flushPromises();
    expect(wrapper.get("button[data-document-id='1']").text()).toContain("기존 문서");
  });

  it("늦은 chunk 응답이 새 문서 선택을 덮지 않는다", async () => {
    let resolveChunks!: (value: object[]) => void;
    api.fetchKnowledgeChunks.mockReturnValue(new Promise(resolve => { resolveChunks = resolve; }));
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    await wrapper.get("button[data-document-id='1']").trigger("click");
    await wrapper.findAll("button").find(button => button.text() === "새 문서")!.trigger("click");
    resolveChunks([{ id: 9, sequenceNo: 0, searchStatus: "INDEXED", startOffset: 0, endOffset: 2, content: "오래된 문단" }]);
    await flushPromises();
    expect(wrapper.text()).not.toContain("오래된 문단");
  });

  it("선택 항목을 native button과 aria-pressed로 표현한다", async () => {
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    const button = wrapper.get("button[data-document-id='1']");
    expect(button.attributes("aria-pressed")).toBe("false");
    await button.trigger("click");
    expect(button.attributes("aria-pressed")).toBe("true");
  });

  it("필터 변경 전에 시작한 chunk 응답을 무시하고 선택을 초기화한다", async () => {
    let resolveChunks!: (value: object[]) => void;
    api.fetchKnowledgeChunks.mockReturnValue(new Promise(resolve => { resolveChunks = resolve; }));
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    await wrapper.get("button[data-document-id='1']").trigger("click");
    await wrapper.get(".admin-toolbar select").setValue("DRAFT");
    await flushPromises();
    resolveChunks([{ id: 9, sequenceNo: 0, searchStatus: "INDEXED", startOffset: 0, endOffset: 2, content: "오래된 문단" }]);
    await flushPromises();

    expect(wrapper.find("h2").text()).toBe("새 문서");
    expect(wrapper.text()).not.toContain("오래된 문단");
  });

  it("chunk 재시도를 시작하면 이전 오류와 빈 상태를 숨기고 성공 시 복구한다", async () => {
    let resolveRetry!: (value: object[]) => void;
    api.fetchKnowledgeChunks
      .mockRejectedValueOnce(new Error("temporary"))
      .mockReturnValueOnce(new Promise(resolve => { resolveRetry = resolve; }));
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    await wrapper.get("button[data-document-id='1']").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("검색 문단을 불러오지 못했습니다.");
    await wrapper.find(".admin-chunks .admin-error button").trigger("click");

    expect(wrapper.text()).not.toContain("검색 문단을 불러오지 못했습니다.");
    expect(wrapper.text()).not.toContain("아직 생성된 검색 문단이 없습니다.");
    resolveRetry([{ id: 10, sequenceNo: 0, searchStatus: "INDEXED", startOffset: 0, endOffset: 2, content: "복구 문단" }]);
    await flushPromises();
    expect(wrapper.text()).toContain("복구 문단");
  });
});
