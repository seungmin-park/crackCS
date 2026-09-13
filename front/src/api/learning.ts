import { get } from "@/api/client";
import type { EvaluationStatus, EvaluationVerdict } from "@/api/answers";

export type KnowledgeStatus = "UNKNOWN" | "LEARNING" | "STABLE";
export type ConceptKnowledge = {
  conceptId: number;
  conceptName: string;
  status: KnowledgeStatus;
  masteryScore: number | null;
  confidenceScore: number;
  attemptCount: number;
  lastEvaluatedAt: string | null;
};
export type TopicKnowledge = {
  topicId: number;
  topicName: string;
  status: KnowledgeStatus;
  masteryScore: number | null;
  confidenceScore: number;
  unknownCount: number;
  learningCount: number;
  stableCount: number;
  concepts: ConceptKnowledge[];
};
export type Recommendation = {
  questionId: number | null;
  title: string | null;
  conceptId: number | null;
  conceptName: string | null;
  reason: "UNASSESSED_CONCEPT" | "LOW_MASTERY" | "NO_AVAILABLE_QUESTION";
  reasonText: string;
};
export type RecentEvaluation = {
  answerId: number;
  questionTitle: string;
  status: EvaluationStatus;
  verdict: EvaluationVerdict | null;
  score: number | null;
  submittedAt: string;
};
export type LearningProgress = {
  totalAnswers: number;
  recentAnswerCount: number;
  recentEvaluations: RecentEvaluation[];
  topics: TopicKnowledge[];
  recommendation: Recommendation;
};

export function fetchKnowledgeStates(): Promise<{ topics: TopicKnowledge[] }> {
  return get("/api/members/me/knowledge-states");
}

export function fetchProgress(): Promise<LearningProgress> {
  return get("/api/members/me/progress");
}
