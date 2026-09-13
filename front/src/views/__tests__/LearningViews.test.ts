import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import LearningHomeView from "@/views/LearningHomeView.vue";
import KnowledgeMapView from "@/views/KnowledgeMapView.vue";

const { fetchProgress, fetchKnowledgeStates } = vi.hoisted(() => ({
  fetchProgress: vi.fn(), fetchKnowledgeStates: vi.fn(),
}));
vi.mock("@/api/learning", () => ({ fetchProgress, fetchKnowledgeStates }));
const global = { stubs: { RouterLink: { props: ["to"], template: '<a :href="to"><slot /></a>' } } };
enableAutoUnmount(afterEach);

function topic() {
  return {
    topicId: 1, topicName: "네트워크", status: "LEARNING", masteryScore: 0, confidenceScore: 12.5,
    unknownCount: 1, learningCount: 1, stableCount: 0,
    concepts: [
      { conceptId: 11, conceptName: "DNS", status: "UNKNOWN", masteryScore: null, confidenceScore: 0, attemptCount: 0, lastEvaluatedAt: null },
      { conceptId: 12, conceptName: "TCP", status: "LEARNING", masteryScore: 0, confidenceScore: 25, attemptCount: 1, lastEvaluatedAt: "2026-09-13T10:00:00" },
    ],
  };
}
function progress() {
  return {
    totalAnswers: 0, recentAnswerCount: 0, recentEvaluations: [], topics: [],
    recommendation: { questionId: 7, title: "DNS는 어떤 역할을 하나요?", conceptId: 11, conceptName: "DNS", reason: "UNASSESSED_CONCEPT", reasonText: "아직 평가하지 않은 DNS 개념입니다." },
  };
}

describe("개인 학습 화면", () => {
  beforeEach(() => { fetchProgress.mockReset(); fetchKnowledgeStates.mockReset(); });

  it("신규 회원에게 첫 학습 안내와 추천 이유 및 문제 링크를 표시한다", async () => {
    fetchProgress.mockResolvedValue(progress());
    const wrapper = mount(LearningHomeView, { global });
    await flushPromises();

    expect(wrapper.text()).toContain("첫 답변");
    expect(wrapper.text()).toContain("먼저 살펴볼 개념: DNS");
    expect(wrapper.text()).toContain("아직 평가하지 않은 DNS 개념입니다.");
    expect(wrapper.get('a[href="/questions/7"]').text()).toContain("DNS는 어떤 역할을 하나요?");
  });

  it("미평가 개념과 평가한 0점 개념을 서로 다르게 표시한다", async () => {
    fetchKnowledgeStates.mockResolvedValue({ topics: [topic()] });
    const wrapper = mount(KnowledgeMapView, { global });
    await flushPromises();

    const dns = wrapper.get('[data-concept-id="11"]');
    const tcp = wrapper.get('[data-concept-id="12"]');
    expect(dns.text()).toContain("미평가");
    expect(dns.text()).toContain("신뢰도 0 / 100");
    expect(dns.text()).not.toContain("숙련도 0");
    expect(tcp.text()).toContain("학습 중");
    expect(tcp.text()).toContain("숙련도 0");
    expect(tcp.text()).toContain("신뢰도 25");
    expect(tcp.text()).toContain("평가 1회");
    expect(tcp.get("time").attributes("datetime")).toBe("2026-09-13T10:00:00");
    expect(wrapper.text()).toContain("정답 확률");
  });

  it("안정 상태의 점수와 신뢰도를 백분율로 다시 곱하지 않는다", async () => {
    const stable = topic();
    Object.assign(stable.concepts[1]!, { status: "STABLE", masteryScore: 80, confidenceScore: 75, attemptCount: 3 });
    fetchKnowledgeStates.mockResolvedValue({ topics: [stable] });
    const wrapper = mount(KnowledgeMapView, { global });
    await flushPromises();

    const concept = wrapper.get('[data-concept-id="12"]');
    expect(concept.text()).toContain("안정");
    expect(concept.text()).toContain("숙련도 80");
    expect(concept.text()).toContain("신뢰도 75");
    expect(concept.text()).not.toContain("7500");
  });

  it("요청 중에는 빈 결과 대신 불러오는 상태를 표시한다", () => {
    fetchProgress.mockReturnValue(new Promise(() => {}));
    const wrapper = mount(LearningHomeView, { global });

    expect(wrapper.get('[aria-busy="true"]').text()).toContain("불러오는 중");
    expect(wrapper.text()).not.toContain("첫 답변");
  });

  it("학습 홈 조회 실패 후 재시도하면 추천 내용을 복구한다", async () => {
    fetchProgress.mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce(progress());
    const wrapper = mount(LearningHomeView, { global });
    await flushPromises();
    expect(wrapper.text()).toContain("불러오지 못했어요");

    await wrapper.get("button").trigger("click");
    await flushPromises();

    expect(wrapper.get('a[href="/questions/7"]').exists()).toBe(true);
    expect(wrapper.text()).not.toContain("불러오지 못했어요");
  });

  it("지식 지도 실패 후 재시도해 빈 주제 안내를 표시한다", async () => {
    fetchKnowledgeStates.mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce({ topics: [] });
    const wrapper = mount(KnowledgeMapView, { global });
    await flushPromises();
    expect(wrapper.text()).toContain("불러오지 못했어요");

    await wrapper.get("button").trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("아직 학습할 개념이 없어요");
  });

  it("최근 풀이 수와 평가 중 및 완료된 답변을 구분해 표시한다", async () => {
    fetchProgress.mockResolvedValue({ ...progress(), totalAnswers: 9, recentAnswerCount: 2, topics: [topic()], recentEvaluations: [
      { answerId: 31, questionTitle: "TCP 연결", status: "EVALUATED", verdict: "INCORRECT", score: 0, submittedAt: "2026-09-13T10:00:00" },
      { answerId: 30, questionTitle: "DNS 캐시", status: "PROCESSING", verdict: null, score: null, submittedAt: "2026-09-12T10:00:00" },
    ] });
    const wrapper = mount(LearningHomeView, { global });
    await flushPromises();

    expect(wrapper.text()).toContain("전체 풀이 9");
    expect(wrapper.text()).toContain("최근 7일 2");
    expect(wrapper.get('a[href="/answers/31"]').text()).toContain("오답");
    expect(wrapper.get('a[href="/answers/30"]').text()).toContain("평가 중");
    expect(wrapper.text()).toContain("네트워크");
  });

  it("추천 후보가 없으면 이유를 표시하고 잘못된 문제 링크를 만들지 않는다", async () => {
    fetchProgress.mockResolvedValue({ ...progress(), recommendation: {
      questionId: null, title: null, conceptId: null, conceptName: null,
      reason: "NO_AVAILABLE_QUESTION", reasonText: "현재 추천할 수 있는 공개 문제가 없습니다.",
    } });
    const wrapper = mount(LearningHomeView, { global });
    await flushPromises();

    expect(wrapper.text()).toContain("현재 추천할 수 있는 공개 문제가 없습니다.");
    expect(wrapper.find('a[href="/questions/null"]').exists()).toBe(false);
    expect(wrapper.get('a[href="/questions"]').exists()).toBe(true);
  });
});
