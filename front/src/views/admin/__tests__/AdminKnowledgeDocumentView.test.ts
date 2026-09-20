import {
  enableAutoUnmount,
  flushPromises,
  mount
} from "@vue/test-utils";
import {
  reactive
} from "vue";
import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi
} from "vitest";

enableAutoUnmount(afterEach);

const route = reactive({
  query: {} as Record<string,
    string>
});
const push = vi.fn(async ({
  query
}: {
  query: Record<string,
    string>;
}) => {
  route.query = query;
});
const replace = vi.fn(async ({
  query
}: {
  query: Record<string,
    string>;
}) => {
  route.query = query;
});
vi.mock("vue-router", () => ({
  useRoute: () => route,
  useRouter: () => ({
    push,
    replace
  })
}));

const api = vi.hoisted(() => ({
  fetchTopics: vi.fn(),
  fetchKnowledgeDocuments: vi.fn(),
  fetchKnowledgeChunks: vi.fn(),
  updateKnowledgeDocument: vi.fn(),
  createKnowledgeDocument: vi.fn(),
  createKnowledgeDocumentVersion: vi.fn(),
  reviewKnowledgeDocument: vi.fn(),
  publishKnowledgeDocument: vi.fn(),
  retireKnowledgeDocument: vi.fn(),
  generateKnowledgeChunks: vi.fn(),
}));
vi.mock("@/api/admin/topics", () => ({
  fetchTopics: api.fetchTopics
}));
vi.mock("@/api/admin/knowledgeDocuments", async importOriginal => ({
  ...(await importOriginal<typeof import("@/api/admin/knowledgeDocuments")>()),
  ...api,
}));
import AdminKnowledgeDocumentView from "@/views/admin/AdminKnowledgeDocumentView.vue";

const document = {
  id: 1,
  topicId: 2,
  title: "기존 문서",
  sourceType: "OFFICIAL_DOC",
  sourceUrl: null,
  technologyVersion: null,
  licenseNote: null,
  content: "본문",
  status: "PUBLISHED",
  checksum: "sum",
  versionSeriesId: "series",
  documentVersion: 1
};

describe("관리자 근거 문서 화면", () => {
  beforeEach(() => {
    Object.values(api).forEach(mock => mock.mockReset());
    route.query = {};
    push.mockClear();
    replace.mockClear();
    api.fetchTopics.mockResolvedValue({
      content: [{
        id: 2,
        name: "네트워크",
        active: true
      }],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1
    });
    api.fetchKnowledgeDocuments.mockResolvedValue({
      content: [document],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1
    });
    api.fetchKnowledgeChunks.mockResolvedValue([]);
  });

  it.each([
    ["등록", "createKnowledgeDocument", undefined, undefined],
    ["수정", "updateKnowledgeDocument", "DRAFT", undefined],
    ["검수", "reviewKnowledgeDocument", "DRAFT", "검수"],
    ["공개", "publishKnowledgeDocument", "DRAFT", "공개"],
    ["폐기", "retireKnowledgeDocument", "PUBLISHED", "폐기"],
    ["새 버전", "createKnowledgeDocumentVersion", "PUBLISHED", "현재 입력으로 새 버전"],
    ["문단 생성", "generateKnowledgeChunks", "PUBLISHED", "검색 문단 생성"],
  ] as const)("%s 실패 시 선택과 모든 입력을 보존한다", async (_, method, status, buttonText) => {
    api[method].mockRejectedValue(new Error("요청 실패"));
    api.fetchKnowledgeDocuments.mockResolvedValue({
      content: [{
        ...document,
        status
      }],
      page: 0,
      totalPages: 1,
      totalElements: 1
    });
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    if (status) {
      await wrapper.get("button[data-document-id='1']").trigger("click");
      await flushPromises();
    }
    const inputs = wrapper.findAll(".admin-form input");
    await inputs[0]!.setValue("보존할 제목");
    await inputs[1]!.setValue("https://example.com/doc");
    await inputs[2]!.setValue("Java 21");
    await wrapper.findAll("textarea")[0]!.setValue("라이선스");
    await wrapper.findAll("textarea")[1]!.setValue("보존할 원문");
    const heading = wrapper.get("h2").text();
    if (buttonText) await wrapper.findAll("button").find(button => button.text() ===
      buttonText)!.trigger("click");
    else await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(api[method]).toHaveBeenCalledOnce();
    expect(wrapper.get("h2").text()).toBe(heading);
    expect(inputs.map(field => field.element.value)).toEqual(["보존할 제목",
      "https://example.com/doc", "Java 21"
    ]);
    expect(wrapper.findAll("textarea").map(field => field.element.value)).toEqual(["라이선스",
      "보존할 원문"
    ]);
    expect(api.fetchKnowledgeDocuments).toHaveBeenCalledOnce();
    expect(wrapper.get('[role="alert"]').text()).not.toBe("");
  });

  it.each([
    ["등록", "createKnowledgeDocument", undefined, undefined],
    ["수정", "updateKnowledgeDocument", "DRAFT", undefined],
    ["검수", "reviewKnowledgeDocument", "DRAFT", "검수"],
    ["공개", "publishKnowledgeDocument", "DRAFT", "공개"],
    ["폐기", "retireKnowledgeDocument", "PUBLISHED", "폐기"],
    ["새 버전", "createKnowledgeDocumentVersion", "PUBLISHED", "현재 입력으로 새 버전"],
  ] as const)("%s 성공 뒤 목록 갱신이 저장된 문서와 입력을 초기화하지 않는다", async (_, method, status, buttonText) => {
    api[method].mockResolvedValue({
      ...document,
      id: 30,
      title: "저장된 문서",
      content: "저장된 원문",
      documentVersion: 2
    });
    api.fetchKnowledgeDocuments.mockResolvedValue({
      content: [{
        ...document,
        status
      }],
      page: 0,
      totalPages: 1,
      totalElements: 1
    });
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    if (status) {
      await wrapper.get("button[data-document-id='1']").trigger("click");
      await flushPromises();
    }
    if (buttonText) await wrapper.findAll("button").find(button => button.text() ===
      buttonText)!.trigger("click");
    else await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(api[method]).toHaveBeenCalledOnce();
    expect(api.fetchKnowledgeDocuments).toHaveBeenCalledTimes(2);
    expect(wrapper.get("h2").text()).toBe("저장된 문서 · v2");
    expect(wrapper.findAll("textarea")[1]!.element.value).toBe("저장된 원문");
  });

  it("삭제로 비어진 마지막 문서 page를 유효 page로 replace한다", async () => {
    route.query = {
      page: "2",
      status: "DRAFT"
    };
    api.fetchKnowledgeDocuments.mockResolvedValueOnce({
      content: [],
      page: 2,
      size: 20,
      totalElements: 20,
      totalPages: 1
    });
    mount(AdminKnowledgeDocumentView);
    await flushPromises();
    expect(replace).toHaveBeenCalledWith({
      query: {
        page: "0",
        status: "DRAFT"
      }
    });
  });

  it("URL의 문서 page와 상태를 복원하고 필터 변경 시 첫 페이지 URL로 돌아간다", async () => {
    route.query = {
      page: "1",
      status: "RETIRED"
    };
    api.fetchKnowledgeDocuments.mockResolvedValueOnce({
      content: [document],
      page: 1,
      size: 20,
      totalElements: 41,
      totalPages: 3
    });
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    expect(api.fetchKnowledgeDocuments).toHaveBeenCalledWith({
      status: "RETIRED",
      page: 1,
      size: 20,
      sort: "id,desc"
    });
    await wrapper.get(".admin-toolbar select").setValue("DRAFT");
    expect(push).toHaveBeenCalledWith({
      query: {
        page: "0",
        status: "DRAFT"
      }
    });
  });

  it("브라우저 이동으로 문서 query가 바뀌면 새 조건을 조회한다", async () => {
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    api.fetchKnowledgeDocuments.mockClear();
    route.query = {
      page: "2",
      status: "PUBLISHED"
    };
    await flushPromises();
    expect(api.fetchKnowledgeDocuments).toHaveBeenCalledWith({
      status: "PUBLISHED",
      page: 2,
      size: 20,
      sort: "id,desc"
    });
    wrapper.unmount();
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
    api.fetchKnowledgeChunks.mockReturnValue(new Promise(resolve => {
      resolveChunks = resolve;
    }));
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    await wrapper.get("button[data-document-id='1']").trigger("click");
    await wrapper.findAll("button").find(button => button.text() === "새 문서")!.trigger(
      "click");
    resolveChunks([{
      id: 9,
      sequenceNo: 0,
      searchStatus: "INDEXED",
      startOffset: 0,
      endOffset: 2,
      content: "오래된 문단"
    }]);
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
    api.fetchKnowledgeChunks.mockReturnValue(new Promise(resolve => {
      resolveChunks = resolve;
    }));
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    await wrapper.get("button[data-document-id='1']").trigger("click");
    await wrapper.get(".admin-toolbar select").setValue("DRAFT");
    await flushPromises();
    resolveChunks([{
      id: 9,
      sequenceNo: 0,
      searchStatus: "INDEXED",
      startOffset: 0,
      endOffset: 2,
      content: "오래된 문단"
    }]);
    await flushPromises();

    expect(wrapper.find("h2").text()).toBe("새 문서");
    expect(wrapper.text()).not.toContain("오래된 문단");
  });

  it("chunk 재시도를 시작하면 이전 오류와 빈 상태를 숨기고 성공 시 복구한다", async () => {
    let resolveRetry!: (value: object[]) => void;
    api.fetchKnowledgeChunks
      .mockRejectedValueOnce(new Error("temporary"))
      .mockReturnValueOnce(new Promise(resolve => {
        resolveRetry = resolve;
      }));
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    await wrapper.get("button[data-document-id='1']").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain("검색 문단을 불러오지 못했습니다.");
    await wrapper.find(".admin-chunks .admin-error button").trigger("click");

    expect(wrapper.text()).not.toContain("검색 문단을 불러오지 못했습니다.");
    expect(wrapper.text()).not.toContain("아직 생성된 검색 문단이 없습니다.");
    resolveRetry([{
      id: 10,
      sequenceNo: 0,
      searchStatus: "INDEXED",
      startOffset: 0,
      endOffset: 2,
      content: "복구 문단"
    }]);
    await flushPromises();
    expect(wrapper.text()).toContain("복구 문단");
  });

  it("문단 생성 성공 뒤 늦게 끝난 기존 조회 결과를 무시한다", async () => {
    let resolveChunks!: (value: object[]) => void;
    api.fetchKnowledgeChunks.mockReturnValue(new Promise(resolve => {
      resolveChunks = resolve;
    }));
    api.generateKnowledgeChunks.mockResolvedValue({
      reused: false,
      chunks: [{
        id: 11,
        sequenceNo: 0,
        searchStatus: "INDEXED",
        startOffset: 0,
        endOffset: 4,
        content: "새 문단"
      }],
    });
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    await wrapper.get("button[data-document-id='1']").trigger("click");
    await wrapper.findAll("button").find(button => button.text() === "검색 문단 생성")!
      .trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("새 문단");
    resolveChunks([]);
    await flushPromises();

    expect(wrapper.text()).toContain("새 문단");
    expect(wrapper.text()).not.toContain("아직 생성된 검색 문단이 없습니다.");
  });

  it("문단 생성 성공 뒤 늦게 실패한 기존 조회 오류를 무시한다", async () => {
    let rejectChunks!: (reason: Error) => void;
    api.fetchKnowledgeChunks.mockReturnValue(new Promise((_, reject) => {
      rejectChunks = reject;
    }));
    api.generateKnowledgeChunks.mockResolvedValue({
      reused: false,
      chunks: [{
        id: 12,
        sequenceNo: 0,
        searchStatus: "INDEXED",
        startOffset: 0,
        endOffset: 4,
        content: "생성 문단"
      }],
    });
    const wrapper = mount(AdminKnowledgeDocumentView);
    await flushPromises();
    await wrapper.get("button[data-document-id='1']").trigger("click");
    await wrapper.findAll("button").find(button => button.text() === "검색 문단 생성")!
      .trigger("click");
    await flushPromises();

    rejectChunks(new Error("late failure"));
    await flushPromises();

    expect(wrapper.text()).toContain("생성 문단");
    expect(wrapper.text()).not.toContain("검색 문단을 불러오지 못했습니다.");
  });
});
