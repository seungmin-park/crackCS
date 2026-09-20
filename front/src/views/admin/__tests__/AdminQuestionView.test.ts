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
  fetchConcepts: vi.fn(),
  fetchAdminQuestions: vi.fn(),
  fetchAdminQuestion: vi.fn(),
  replaceQuestionConcepts: vi.fn(),
  createAdminQuestion: vi.fn(),
  updateAdminQuestion: vi.fn(),
  reviewQuestion: vi.fn(),
  publishQuestion: vi.fn(),
  retireQuestion: vi.fn(),
  createQuestionVersion: vi.fn(),
}));

vi.mock("@/api/admin/topics", () => ({
  fetchTopics: api.fetchTopics
}));
vi.mock("@/api/admin/concepts", () => ({
  fetchConcepts: api.fetchConcepts
}));
vi.mock("@/api/admin/questions", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/api/admin/questions")>()),
  ...api,
}));

import AdminQuestionView from "@/views/admin/AdminQuestionView.vue";

const networkQuestion = {
  id: 10,
  topicId: 2,
  origin: "ADMIN" as const,
  type: "NORMAL" as const,
  difficulty: "BASIC" as const,
  content: "TCP 질문",
  referenceAnswer: "TCP 답안",
  status: "DRAFT" as const,
  versionSeriesId: "series-1",
  questionVersion: 1,
  createdByMemberId: 1,
  reviewedByMemberId: null,
  reviewedAt: null,
  concepts: [],
  createdAt: "2026-09-01T00:00:00",
  updatedAt: "2026-09-01T00:00:00",
};

describe("관리자 Question 화면", () => {
  beforeEach(() => {
    Object.values(api).forEach(mock => mock.mockReset());
    route.query = {};
    push.mockClear();
    replace.mockClear();
    api.fetchTopics.mockResolvedValue({
      content: [{
        id: 1,
        parentId: null,
        code: "OS",
        name: "운영체제",
        active: true
      },
      {
        id: 2,
        parentId: null,
        code: "NETWORK",
        name: "네트워크",
        active: true
      },
      ],
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 1,
    });
    api.fetchConcepts.mockResolvedValue({
      content: [{
        id: 11,
        topicId: 1,
        code: "THREAD",
        name: "스레드",
        description: null,
        active: true
      },
      {
        id: 12,
        topicId: 2,
        code: "TCP",
        name: "TCP 신뢰성",
        description: null,
        active: true
      },
      ],
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 1,
    });
    api.fetchAdminQuestions.mockResolvedValue({
      content: [networkQuestion],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
    });
    api.fetchAdminQuestion.mockResolvedValue(networkQuestion);
    api.replaceQuestionConcepts.mockResolvedValue({
      ...networkQuestion,
      concepts: [{
        conceptId: 12,
        code: "TCP",
        name: "TCP 신뢰성",
        weight: 1,
        required: true
      }],
    });
  });

  it("선택이 바뀐 화면에서는 늦은 저장 완료가 목록만 갱신하고 새 입력을 보존한다", async () => {
    let resolveMutation!: (value: typeof networkQuestion) => void;
    api.publishQuestion.mockReturnValue(new Promise(resolve => { resolveMutation = resolve; }));
    api.fetchAdminQuestions.mockResolvedValue({
      content: [{ ...networkQuestion, status: "DRAFT" }], page: 0, size: 20, totalPages: 1, totalElements: 1,
    });
    api.fetchAdminQuestion.mockResolvedValue(networkQuestion);
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await flushPromises();
    await wrapper.findAll("button").find(button => button.text() === "공개")!.trigger("click");
    expect(api.publishQuestion).toHaveBeenCalledOnce();
    route.query = { status: "PUBLISHED", page: "0" };
    await flushPromises();
    await wrapper.findAll("textarea")[0]!.setValue("새로 작성 중");
    api.fetchAdminQuestions.mockClear();
    resolveMutation(networkQuestion);
    await flushPromises();
    expect(api.fetchAdminQuestions).toHaveBeenCalledOnce();
    expect(api.fetchAdminQuestions).toHaveBeenCalledWith(expect.objectContaining({ status: "PUBLISHED" }));
    expect(wrapper.findAll("textarea")[0]!.element.value).toBe("새로 작성 중");
    expect(wrapper.get("h2").text()).toContain("새 문제");
  });

  it("진행 중 목록 응답은 화면 폐기 후 목적지 URL을 교정하지 않는다", async () => {
    let resolveList!: (value: object) => void;
    route.query = { page: "1", status: "DRAFT" };
    api.fetchAdminQuestions.mockReturnValueOnce(new Promise(resolve => { resolveList = resolve; }));
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    wrapper.unmount();
    route.query = { page: "3", status: "ACTIVE" };
    resolveList({ content: [], page: 1, size: 20, totalPages: 0, totalElements: 0 });
    await flushPromises();
    expect(replace).not.toHaveBeenCalled();
    expect(route.query).toEqual({ page: "3", status: "ACTIVE" });
  });

  it.each([
    ["createAdminQuestion", undefined, undefined],
    ["updateAdminQuestion", "DRAFT", undefined],
    ["reviewQuestion", "DRAFT", "검수"],
    ["publishQuestion", "DRAFT", "공개"],
    ["retireQuestion", "PUBLISHED", "폐기"],
    ["createQuestionVersion", "PUBLISHED", "현재 입력으로 새 버전"],
  ] as const)("%s 대기 중 화면을 떠나면 이전 목록과 목적지 URL을 변경하지 않는다", async (method, status, buttonText) => {
    let resolveMutation!: (value: typeof networkQuestion) => void;
    api[method].mockReturnValue(new Promise(resolve => { resolveMutation = resolve; }));
    route.query = { page: "1", status: "DRAFT" };
    api.fetchAdminQuestions.mockResolvedValueOnce({
      content: [{ ...networkQuestion, status }], page: 1, size: 20, totalPages: 2, totalElements: 21,
    });
    api.fetchAdminQuestion.mockResolvedValue({ ...networkQuestion, status });
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    if (status) {
      await wrapper.get("button[data-question-id='10']").trigger("click");
      await flushPromises();
    }
    if (buttonText) await wrapper.findAll("button").find(button => button.text() === buttonText)!.trigger("click");
    else await wrapper.get("form").trigger("submit");
    expect(api[method]).toHaveBeenCalledOnce();
    wrapper.unmount();
    route.query = { page: "3", status: "ACTIVE" };
    api.fetchAdminQuestions.mockClear();
    resolveMutation(networkQuestion);
    await flushPromises();
    expect(api.fetchAdminQuestions).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
    expect(route.query).toEqual({ page: "3", status: "ACTIVE" });
  });

  it.each([
    ["등록", "createAdminQuestion", undefined, undefined],
    ["수정", "updateAdminQuestion", "DRAFT", undefined],
    ["검수", "reviewQuestion", "DRAFT", "검수"],
    ["공개", "publishQuestion", "DRAFT", "공개"],
    ["폐기", "retireQuestion", "PUBLISHED", "폐기"],
    ["새 버전", "createQuestionVersion", "PUBLISHED", "현재 입력으로 새 버전"],
    ["평가 기준", "replaceQuestionConcepts", "DRAFT", "평가 기준 저장"],
  ] as const)("%s 실패 시 선택·입력·평가 기준을 보존한다", async (_, method, status, buttonText) => {
    api[method].mockRejectedValue(new Error("요청 실패"));
    api.fetchAdminQuestion.mockResolvedValue({
      ...networkQuestion,
      status,
      concepts: [{
        conceptId: 12,
        weight: 1,
        required: true
      }]
    });
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    if (status) {
      await wrapper.get("button[data-question-id='10']").trigger("click");
      await flushPromises();
    }
    await wrapper.findAll("textarea")[0]!.setValue("보존할 문제");
    await wrapper.findAll("textarea")[1]!.setValue("보존할 답안");
    const heading = wrapper.get("h2").text();
    if (buttonText) await wrapper.findAll("button").find(button => button.text() ===
      buttonText)!.trigger("click");
    else await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(api[method]).toHaveBeenCalledOnce();
    expect(wrapper.get("h2").text()).toBe(heading);
    expect(wrapper.findAll("textarea").map(field => field.element.value)).toEqual([
      "보존할 문제", "보존할 답안"
    ]);
    expect(wrapper.findAll(".criteria-row")).toHaveLength(status ? 1 : 0);
    expect(api.fetchAdminQuestions).toHaveBeenCalledOnce();
    expect(wrapper.get('[role="alert"]').text()).not.toBe("");
  });

  it.each([
    ["등록", "createAdminQuestion", undefined, undefined],
    ["수정", "updateAdminQuestion", "DRAFT", undefined],
    ["검수", "reviewQuestion", "DRAFT", "검수"],
    ["공개", "publishQuestion", "DRAFT", "공개"],
    ["폐기", "retireQuestion", "PUBLISHED", "폐기"],
    ["새 버전", "createQuestionVersion", "PUBLISHED", "현재 입력으로 새 버전"],
  ] as const)("%s 성공 뒤 목록 갱신이 서버가 반환한 선택을 초기화하지 않는다", async (_, method, status, buttonText) => {
    api[method].mockResolvedValue({
      ...networkQuestion,
      id: 30,
      questionVersion: 2
    });
    api.fetchAdminQuestion.mockResolvedValue({
      ...networkQuestion,
      status
    });
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    if (status) {
      await wrapper.get("button[data-question-id='10']").trigger("click");
      await flushPromises();
    }
    await wrapper.findAll("textarea")[0]!.setValue("현재 입력");
    if (buttonText) await wrapper.findAll("button").find(button => button.text() ===
      buttonText)!.trigger("click");
    else await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(api[method]).toHaveBeenCalledOnce();
    expect(api.fetchAdminQuestions).toHaveBeenCalledTimes(2);
    expect(wrapper.get("h2").text()).toBe("Question #30 · v2");
    expect(wrapper.findAll("textarea")[0]!.element.value).toBe("현재 입력");
  });

  it("범위를 벗어난 문제 page를 마지막 유효 page로 replace한다", async () => {
    route.query = {
      page: "3",
      status: "PUBLISHED"
    };
    api.fetchAdminQuestions.mockResolvedValueOnce({
      content: [],
      page: 3,
      size: 20,
      totalElements: 1,
      totalPages: 1
    });
    mount(AdminQuestionView);
    await flushPromises();
    expect(replace).toHaveBeenCalledWith({
      query: {
        page: "0",
        status: "PUBLISHED"
      }
    });
  });

  it("새로고침된 URL의 page와 상태로 목록을 조회하고 다음 페이지를 URL에 기록한다", async () => {
    route.query = {
      page: "2",
      status: "PUBLISHED"
    };
    api.fetchAdminQuestions.mockResolvedValueOnce({
      content: [networkQuestion],
      page: 2,
      size: 20,
      totalElements: 70,
      totalPages: 4,
    });
    const wrapper = mount(AdminQuestionView);
    await flushPromises();

    expect(api.fetchAdminQuestions).toHaveBeenCalledWith({
      status: "PUBLISHED",
      page: 2,
      size: 20,
      sort: "id,desc"
    });
    await wrapper.get("button[data-page='next']").trigger("click");
    expect(push).toHaveBeenCalledWith({
      query: {
        page: "3",
        status: "PUBLISHED"
      }
    });
  });

  it("브라우저 이동으로 URL query가 바뀌면 해당 page와 상태를 다시 조회한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    api.fetchAdminQuestions.mockClear();

    route.query = {
      page: "3",
      status: "RETIRED"
    };
    await flushPromises();

    expect(api.fetchAdminQuestions).toHaveBeenCalledWith({
      status: "RETIRED",
      page: 3,
      size: 20,
      sort: "id,desc"
    });
    wrapper.unmount();
  });

  it("상태 필터를 바꾸면 선택을 지우고 page를 0으로 되돌린 URL을 기록한다", async () => {
    route.query = {
      page: "2"
    };
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await flushPromises();

    await wrapper.get(".admin-toolbar select").setValue("PUBLISHED");
    await flushPromises();
    expect(push).toHaveBeenCalledWith({
      query: {
        page: "0",
        status: "PUBLISHED"
      }
    });
    expect(wrapper.find("h2").text()).toBe("새 문제");
  });

  it("Concept 후보를 마지막 페이지까지 순차 조회하고 실패한 후보 조회를 재시도한다", async () => {
    api.fetchConcepts
      .mockResolvedValueOnce({
        content: [],
        page: 0,
        size: 100,
        totalElements: 101,
        totalPages: 2
      })
      .mockRejectedValueOnce(new Error("temporary"))
      .mockResolvedValueOnce({
        content: [],
        page: 0,
        size: 100,
        totalElements: 101,
        totalPages: 2
      })
      .mockResolvedValueOnce({
        content: [{
          id: 112,
          topicId: 2,
          code: "LATE",
          name: "후반 후보",
          active: true
        }],
        page: 1,
        size: 100,
        totalElements: 101,
        totalPages: 2
      });
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    expect(wrapper.text()).toContain("관계 후보를 불러오지 못했습니다.");

    await wrapper.get("button[data-retry='relations']").trigger("click");
    await flushPromises();
    expect(api.fetchConcepts).toHaveBeenLastCalledWith({
      active: true,
      page: 1,
      size: 100
    });
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await flushPromises();
    expect(wrapper.find('select[aria-label="추가할 Concept"]').text()).toContain("후반 후보");
  });

  it("늦은 상세 응답이 새 문제 입력과 생성 대상을 바꾸지 않는다", async () => {
    let resolveDetail!: (value: typeof networkQuestion) => void;
    api.fetchAdminQuestion.mockReturnValue(new Promise<typeof networkQuestion>(
      resolve => {
        resolveDetail = resolve;
      }));
    api.createAdminQuestion.mockResolvedValue({
      ...networkQuestion,
      id: 20,
      content: "새 문제"
    });
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await wrapper.findAll("button").find(button => button.text() === "새 문제")!.trigger(
      "click");
    await wrapper.find(".admin-form select").setValue("2");
    await wrapper.findAll("textarea")[0]!.setValue("새 문제");
    await wrapper.findAll("textarea")[1]!.setValue("새 답안");

    resolveDetail(networkQuestion);
    await flushPromises();

    expect(wrapper.find("h2").text()).toBe("새 문제");
    expect(wrapper.findAll("textarea")[0]!.element.value).toBe("새 문제");
    await wrapper.find("form").trigger("submit");
    await flushPromises();
    expect(api.createAdminQuestion).toHaveBeenCalledWith({
      topicId: 2,
      difficulty: "BASIC",
      content: "새 문제",
      referenceAnswer: "새 답안"
    });
    expect(api.updateAdminQuestion).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it("초기 목록 실패 뒤 재시도하면 로딩을 끝내고 목록을 표시한다", async () => {
    api.fetchAdminQuestions.mockRejectedValueOnce(new Error("temporary"));
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    expect(wrapper.text()).toContain("문제 목록을 불러오지 못했습니다.");
    expect(wrapper.text()).not.toContain("문제를 불러오는 중");

    await wrapper.get("button[data-retry='list']").trigger("click");
    await flushPromises();
    expect(wrapper.get("button[data-question-id='10']").text()).toContain("TCP 질문");
  });

  it("선택 항목을 native button과 aria-pressed로 표현한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    const button = wrapper.get("button[data-question-id='10']");
    expect(button.attributes("aria-pressed")).toBe("false");
    await button.trigger("click");
    await flushPromises();
    expect(button.attributes("aria-pressed")).toBe("true");
  });

  it("저장 중 새 문제로 전환해도 완료 응답이 새 입력을 덮지 않는다", async () => {
    let resolveUpdate!: (value: typeof networkQuestion) => void;
    api.updateAdminQuestion.mockReturnValue(new Promise(resolve => {
      resolveUpdate = resolve;
    }));
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await flushPromises();
    await wrapper.find("form").trigger("submit");
    await wrapper.findAll("button").find(button => button.text() === "새 문제")!.trigger(
      "click");
    await wrapper.findAll("textarea")[0]!.setValue("새 입력");
    resolveUpdate({
      ...networkQuestion,
      content: "서버 수정본"
    });
    await flushPromises();

    expect(wrapper.find("h2").text()).toBe("새 문제");
    expect(wrapper.findAll("textarea")[0]!.element.value).toBe("새 입력");
  });

  it("필터 변경 전에 시작한 상세 응답을 무시한다", async () => {
    let resolveDetail!: (value: typeof networkQuestion) => void;
    api.fetchAdminQuestion.mockReturnValue(new Promise(resolve => {
      resolveDetail = resolve;
    }));
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await wrapper.get(".admin-toolbar select").setValue("PUBLISHED");
    await flushPromises();

    resolveDetail(networkQuestion);
    await flushPromises();

    expect(wrapper.find("h2").text()).toBe("새 문제");
    expect(wrapper.findAll("textarea")[0]!.element.value).toBe("");
  });

  it("선택한 문제와 같은 Topic의 Concept만 평가 기준 후보로 표시한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await flushPromises();

    const options = wrapper.find('select[aria-label="추가할 Concept"]').findAll("option");
    expect(options.map(option => option.text())).toEqual(["선택", "TCP 신뢰성"]);
    expect(wrapper.text()).not.toContain("Concept 추가\n스레드");
  });

  it("명시적으로 고른 Concept과 기본 평가 정책을 저장한다", async () => {
    const wrapper = mount(AdminQuestionView);
    await flushPromises();
    await wrapper.get("button[data-question-id='10']").trigger("click");
    await flushPromises();

    const addButton = wrapper.findAll("button").find(button => button.text() ===
      "Concept 추가")!;
    expect(addButton.attributes("disabled")).toBeDefined();
    await wrapper.find('select[aria-label="추가할 Concept"]').setValue("12");
    await addButton.trigger("click");
    await wrapper.findAll("button").find(button => button.text() === "평가 기준 저장")!
      .trigger("click");
    await flushPromises();

    expect(api.replaceQuestionConcepts).toHaveBeenCalledWith(10, [{
      conceptId: 12,
      weight: 1,
      required: true
    },]);
    expect(wrapper.text()).toContain("평가 Concept을 교체했습니다.");
  });
});
